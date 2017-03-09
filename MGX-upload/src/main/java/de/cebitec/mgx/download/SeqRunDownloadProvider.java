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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqRunDownloadProvider implements DownloadProviderI<SequenceDTOList> {

    protected final String projectName;
    private final GPMSManagedDataSourceI dataSource;
    protected SeqReaderI<? extends DNASequenceI> reader;
    protected long lastAccessed;
    protected int maxSeqsPerChunk = 200;

    public SeqRunDownloadProvider(GPMSManagedDataSourceI dataSource, String projName, String dbFile) throws MGXException {
        this.projectName = projName;
        this.dataSource = dataSource;
        this.dataSource.subscribe();

        try {
            reader = SeqReaderFactory.<DNASequenceI>getReader(dbFile);
        } catch (SeqStoreException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }

        lastAccessed = System.currentTimeMillis();
    }

    protected GPMSManagedConnectionI getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void cancel() {
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
        dataSource.close();

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
    public SequenceDTOList fetch() throws MGXException {
        int count = 0;
        try {
            //
            // fetch sequences
            //
            List<DNASequenceI> seqs = new LinkedList<>();
            while (count <= maxSeqsPerChunk && reader.hasMoreElements()) {
                DNASequenceI seq = reader.nextElement();
                seqs.add(seq);
                count++;
            }

            //
            // get names from database
            //
            if (!seqs.isEmpty()) {
                getSequenceNames(seqs);

                // additional check
                for (DNASequenceI seq : seqs) {
                    if (seq.getName() == null) {
                        throw new MGXException("No name for read id " + seq.getId() + " in project " + projectName);
                    }
                }
            }
            //
            // convert to DTO
            //
            Builder listBuilder = SequenceDTOList.newBuilder();
            for (DNASequenceI seq : seqs) {
                SequenceDTO.Builder dtob = SequenceDTO.newBuilder()
                        .setId(seq.getId())
                        .setNameBytes(ByteString.copyFrom(seq.getName()))
                        .setSequenceBytes(ByteString.copyFrom(seq.getSequence()));

                if (seq instanceof DNAQualitySequenceI) {
                    dtob.setQuality(ByteString.copyFrom(((DNAQualitySequenceI) seq).getQuality()));
                }
                listBuilder.addSeq(dtob.build());
            }
            lastAccessed = System.currentTimeMillis();
            listBuilder.setComplete(!reader.hasMoreElements());
            return listBuilder.build();

        } catch (SeqStoreException ex) {
            throw new MGXException(ex);
        }

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
        
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(buildSQL(seqs.size()))) {
                int idx = 1;
                for (DNASequenceI seq : seqs) {
                    stmt.setLong(idx++, seq.getId());
                }

                Iterator<DNASequenceI> seqIter = seqs.iterator();

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next() && seqIter.hasNext()) {
                        long seqId = rs.getLong(1);
                        DNASequenceI seq = seqIter.next();
                        
                        //assert seqId == seq.getId();
                        seq.setName(rs.getString(2).getBytes());
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
        StringBuilder sb = new StringBuilder("SELECT id, name FROM read WHERE id IN (?");
        for (int i = 1; i < numElements; i++) {
            sb.append(",?");
        }
        sb.append(") ORDER BY id ASC");
        return sb.toString();
    }
}
