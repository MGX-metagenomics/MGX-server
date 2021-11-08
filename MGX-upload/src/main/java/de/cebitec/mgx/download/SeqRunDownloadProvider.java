package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.util.GPMSManagedConnectionI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.seqcompression.FourBitEncoder;
import de.cebitec.mgx.seqcompression.QualityEncoder;
import de.cebitec.mgx.seqcompression.SequenceException;
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
    protected final static int BASE_PAIR_LIMIT = 2_000_000;

    protected volatile MGXException exception = null;
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
        if (exception != null) {
            return;
        }

        if (!lock.tryLock()) {
            return;
        }

        if (nextChunk == null) {

            List<DNASequenceI> xferList = new LinkedList<>();
            int current_bp = 0;
            //
            // fetch sequences
            //
            try {
                while (reader.hasMoreElements() && xferList.size() < maxSeqsPerChunk && current_bp < BASE_PAIR_LIMIT) {
                    DNASequenceI seq = reader.nextElement();
                    current_bp += seq.getSequence().length;
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
                            exception = new MGXException("No name for read id " + seq.getId() + " in project " + projectName);
                            lock.unlock();
                            return;
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
                            .setSequence(ByteString.copyFrom(FourBitEncoder.encode(seq.getSequence())));

                    if (seq instanceof DNAQualitySequenceI) {
                        DNAQualitySequenceI qseq = (DNAQualitySequenceI) seq;
                        dtob.setQuality(ByteString.copyFrom(QualityEncoder.encode(qseq.getQuality())));
                    }
                    listBuilder.addSeq(dtob.build());
                }
                listBuilder.setComplete(!reader.hasMoreElements());
                nextChunk = listBuilder.build();

            } catch (SQLException | SequenceException ex) {
                exception = new MGXException(ex);
            }
        }

        lock.unlock();

    }

    @Override
    public SequenceDTOList fetch() throws MGXException {

        if (exception != null) {
            throw exception;
        }

        if (nextChunk == null) {
            // no data yet, return empty chunk
            SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
            listBuilder.setComplete(false);
            lastAccessed = System.currentTimeMillis();
            return listBuilder.build();
            // if run() did not run and produce a new chunk, we synchronously
            // invoke it to obtain the data; race conditions avoided via the
            // lock
            //run();
        }

        lock.lock();
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

    private void getSequenceNames(List<DNASequenceI> seqs) throws SQLException {
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

}
