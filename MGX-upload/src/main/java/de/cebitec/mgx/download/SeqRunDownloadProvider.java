package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.util.GPMSManagedConnectionI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqRunDownloadProvider implements DownloadProviderI<SequenceDTOList>, Runnable {

    protected final String projectName;
    private final GPMSManagedDataSourceI dataSource;
    protected SeqReaderI<? extends DNASequenceI> reader;
    protected long lastAccessed;
    protected int maxSeqsPerChunk = 200;

    protected State state = State.OK;
    protected MGXException exception = null;
    protected final Lock lock = new ReentrantLock();
    protected volatile SequenceDTOList nextChunk = null;

    public SeqRunDownloadProvider(GPMSManagedDataSourceI dataSource, String projName, String dbFile, int chunkSize) throws MGXException {
        this(dataSource, projName, dbFile);
        maxSeqsPerChunk = chunkSize;
    }

    public SeqRunDownloadProvider(GPMSManagedDataSourceI dataSource, String projName, String dbFile) throws MGXException {
        this.projectName = projName;
        this.dataSource = dataSource;

        try {
            reader = SeqReaderFactory.<DNASequenceI>getReader(dbFile);
        } catch (SeqStoreException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }

        this.dataSource.subscribe(this);

        lastAccessed = System.currentTimeMillis();
    }

    protected GPMSManagedConnectionI getConnection() throws SQLException {
        return dataSource.getConnection(this);
    }

    @Override
    public void cancel() {
        dataSource.close(this);
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (SeqStoreException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() throws MGXException {
        dataSource.close(this);

        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (SeqStoreException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        lock.lock();

        if (nextChunk == null) {

            List<DNASequenceI> xferList = new LinkedList<>();
            //
            // fetch sequences
            //
            try {
                while (xferList.size() < maxSeqsPerChunk && reader.hasMoreElements()) {
                    DNASequenceI seq = reader.nextElement();
                    xferList.add(seq);
                }

                //
                // get names from database
                //
                if (!xferList.isEmpty()) {
                    getSequenceNames(xferList);

                    // additional check
                    for (DNASequenceI seq : xferList) {
                        if (seq.getName() == null) {
                            state = State.ERROR;
                            throw new MGXException("No name for read id " + seq.getId() + " in project " + projectName);
                        }
                    }
                }


                //
                // convert to DTO
                //
                Builder listBuilder = SequenceDTOList.newBuilder();
                for (DNASequenceI seq : xferList) {
                    SequenceDTO.Builder dtob = SequenceDTO.newBuilder()
                            .setId(seq.getId())
                            .setNameBytes(ByteString.copyFrom(seq.getName()))
                            .setSequenceBytes(ByteString.copyFrom(seq.getSequence()));

                    if (seq instanceof DNAQualitySequenceI) {
                        dtob.setQuality(ByteString.copyFrom(((DNAQualitySequenceI) seq).getQuality()));
                    }
                    listBuilder.addSeq(dtob.build());
                }
                listBuilder.setComplete(!reader.hasMoreElements());
                nextChunk = listBuilder.build();

            } catch (MGXException | SeqStoreException ex) {
                exception = (MGXException) (ex instanceof MGXException ? ex : new MGXException(ex));
                state = State.ERROR;
            }
        }

        lock.unlock();

    }

    @Override
    public SequenceDTOList fetch() throws MGXException {

        if (state != State.OK) {
            throw exception;
        }

        lock.lock();
        if (nextChunk == null) {
            // if run() did not run and produce a new chunk, we synchronously
            // invoke it to obtain the data; race conditions avoided via the
            // lock
            run();
        }
        SequenceDTOList ret = nextChunk;
        nextChunk = null;
        lock.unlock();

        lastAccessed = System.currentTimeMillis();
        return ret;
    }

    @Override
    public String getProjectName() {
        lastAccessed = System.currentTimeMillis();
        return projectName;
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

    private void getSequenceNames(List<DNASequenceI> seqs) throws MGXException {
        //
        // sort by id, ascending
        //
        Collections.sort(seqs, new Comparator<DNASequenceI>() {
            @Override
            public int compare(DNASequenceI o1, DNASequenceI o2) {
                return Long.compare(o1.getId(), o2.getId());
            }

        });

        try (Connection conn = dataSource.getConnection(this)) {
            try (PreparedStatement stmt = conn.prepareStatement(buildSQL(seqs.size()))) {
                int idx = 1;
                for (DNASequenceI seq : seqs) {
                    stmt.setLong(idx++, seq.getId());
                }

                Iterator<DNASequenceI> seqIter = seqs.iterator();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next() && seqIter.hasNext()) {
                        DNASequenceI seq = seqIter.next();
                        seq.setName(rs.getString(1).getBytes());
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }

    }

    private static String buildSQL(int numElements) {
        assert numElements > 0;
        StringBuilder sb = new StringBuilder("SELECT name FROM read WHERE id IN (?");
        for (int i = 1; i < numElements; i++) {
            sb.append(",?");
        }
        sb.append(") ORDER BY id ASC");
        return sb.toString();
    }

    protected static enum State {
        OK,
        ERROR;
    }
}
