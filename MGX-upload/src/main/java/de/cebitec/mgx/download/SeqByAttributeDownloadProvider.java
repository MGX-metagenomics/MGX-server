package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqStoreException;
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

    private static final String GET_SEQS = "SELECT DISTINCT ON (r.id) r.id, r.name "
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

        long qTime = System.currentTimeMillis();
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
                throw new MGXException(ex);
            }

            qTime = System.currentTimeMillis() - qTime;
            System.out.println("initial query took " + qTime);
        }

        qTime = System.currentTimeMillis();
        SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
        readnames.clear();
        int count = 0;
        try {
            while (count < maxSeqsPerChunk && rs.next()) {
                readnames.put(rs.getLong(1), rs.getString(2));
                count++;
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        have_more_data = count == maxSeqsPerChunk;

        long[] ids = readnames.keys();

        try {
            for (DNASequenceI seq : reader.fetch(ids)) {
                SequenceDTO.Builder dtob = SequenceDTO.newBuilder()
                        .setId(seq.getId())
                        .setName(readnames.remove(seq.getId()))
                        .setSequence(new String(seq.getSequence()));
                if (seq instanceof DNAQualitySequenceI) {
                    DNAQualitySequenceI qseq = (DNAQualitySequenceI) seq;
                    dtob.setQuality(ByteString.copyFrom(qseq.getQuality()));
                }
                listBuilder.addSeq(dtob.build());
            }
        } catch (SeqStoreException ex) {
            throw new MGXException(ex);
        }

        qTime = System.currentTimeMillis() - qTime;
        System.out.println("chunk fill took " + qTime);

        lastAccessed = System.currentTimeMillis();
        listBuilder.setComplete(!have_more_data);
        return listBuilder.build();
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
        sb.append(") ORDER BY r.id ASC");
        return sb.toString();
    }
}
