package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.seqcompression.FourBitEncoder;
import de.cebitec.mgx.seqcompression.QualityEncoder;
import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.DNASequenceI;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class SeqByAttributeDownloadProvider extends SeqRunDownloadProvider {

    private static final String GET_SEQS = "SELECT r.id, r.name "
            + "FROM observation o JOIN read r ON (o.seq_id = r.id) "
            + "WHERE o.attr_id IN (?";
    private Connection conn = null;
    private PreparedStatement stmt = null;
    private ResultSet rs = null;
    private boolean have_more_data = true;
    private final long[] attributeIDs;
    private TLongObjectMap<String> readnames = null;

    public SeqByAttributeDownloadProvider(GPMSManagedDataSourceI dataSource, String projectName, long[] attributeIDs, String dbFile) throws MGXException {
        super(dataSource, projectName, dbFile);
        this.attributeIDs = attributeIDs;
        this.readnames = new TLongObjectHashMap<>(maxSeqsPerChunk);
        this.lastAccessed = System.currentTimeMillis();
    }

    @Override
    public SequenceDTOList fetch() throws MGXException {

        if (exception != null) {
            throw exception;
        }

        if (lock.tryLock()) {
            if (nextChunk != null) {
                SequenceDTOList ret = nextChunk;
                nextChunk = null;
                lock.unlock();
                lastAccessed = System.currentTimeMillis();
                return ret;
            }
            lock.unlock();
        }

        // no data yet, return empty chunk
        SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
        listBuilder.setComplete(false);
        lastAccessed = System.currentTimeMillis();
        return listBuilder.build();

    }

    @Override
    public void run() {

        if (exception != null) {
            return;
        }

        if (!lock.tryLock()) {
            return;
        }

        if (rs == null) {
            //
            // first invocation, perform DB query
            //
            try {
                conn = getConnection();
                String sql = buildSQLQuery(attributeIDs.length);
                stmt = conn.prepareStatement(sql);
                int pos = 1;
                for (long id : attributeIDs) {
                    stmt.setLong(pos++, id);
                }
                rs = stmt.executeQuery();
            } catch (SQLException ex) {
                exception = new MGXException(ex);
                lock.unlock();
                return;
            }

        }

        if (nextChunk == null) {

            SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
            readnames.clear();
            int count = 0;
            try {
                while (count < maxSeqsPerChunk && rs.next()) {
                    readnames.put(rs.getLong(1), rs.getString(2));
                    count++;
                }
            } catch (SQLException ex) {
                exception = new MGXException(ex);
                lock.unlock();
                return;
            }
            have_more_data = count == maxSeqsPerChunk;

            long[] ids = readnames.keys();

            try {
                for (DNASequenceI seq : reader.fetch(ids)) {
                    byte[] decodedDNA = seq.getSequence();
                    SequenceDTO.Builder dtob = SequenceDTO.newBuilder()
                            .setId(seq.getId())
                            .setName(readnames.remove(seq.getId()))
                            .setSequence(ByteString.copyFrom(FourBitEncoder.encode(decodedDNA)));
                    if (seq instanceof DNAQualitySequenceI) {
                        DNAQualitySequenceI qseq = (DNAQualitySequenceI) seq;
                        dtob.setQuality(ByteString.copyFrom(QualityEncoder.encode(qseq.getQuality())));
                    }
                    listBuilder.addSeq(dtob.build());
                }
            } catch (SequenceException ex) {
                exception = new MGXException(ex);
                lock.unlock();
                return;
            }

            lastAccessed = System.currentTimeMillis();
            listBuilder.setComplete(!have_more_data);
            nextChunk = listBuilder.build();
        }

        lock.unlock();
    }

    @Override
    public void cancel() {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ex) {
        }
        super.cancel();
    }

    @Override
    public void close() throws MGXException {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ex) {
        }
        super.close();
    }

    private static String buildSQLQuery(int numElements) {
        StringBuilder sb = new StringBuilder(GET_SEQS);
        for (int i = 1; i < numElements; i++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }
}
