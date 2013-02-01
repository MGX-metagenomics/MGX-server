package de.cebitec.mgx.download;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class SeqByAttributeDownloadProvider extends SeqRunDownloadProvider {

    private static String GET_SEQS = "SELECT DISTINCT ON (r.id) r.id, r.name "
            + "FROM observation o JOIN read r ON (o.seq_id = r.id) "
            + "WHERE o.attr_id IN (?";
    private PreparedStatement stmt = null;
    private ResultSet rs = null;
    private boolean have_more_data = true;
    private Map<Long, String> readnames = null;

    public SeqByAttributeDownloadProvider(Connection connection, String projectName, Set<Attribute> attrs) throws MGXException {
        super(connection, projectName);
        
        if (attrs.isEmpty()) {
            throw new MGXException("No attributes provided.");
        }
        
        // all attributes are assumed to refer to the same run
        String fName = null;
        for (Attribute a : attrs) {
            if (fName == null) {
                fName = a.getJob().getSeqrun().getDBFile();
            } else {
                String fName2 = a.getJob().getSeqrun().getDBFile();
                if (!fName.equals(fName2)) {
                    throw new MGXException("Selected attributes refer to different sequencing runs.");
                }
            }
        }
        try {
            reader = SeqReaderFactory.getReader(fName);
        } catch (SeqStoreException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }
        
        readnames = new HashMap<>(bulksize);

        try {
            String sql = buildSQLTemplate(attrs.size());
            stmt = conn.prepareStatement(sql);
            int pos = 1;
            for (Attribute a : attrs) {
                stmt.setLong(pos++, a.getId());
            }
            rs = stmt.executeQuery();
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public SequenceDTOList fetch() throws MGXException {
        SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
        readnames.clear();
        int count = 1;
        try {
            while (count <= bulksize && rs.next()) {
                readnames.put(rs.getLong(1), rs.getString(2));
                count++;
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        have_more_data = count == bulksize;
        
        long[] ids = new long[readnames.keySet().size()];
        int i = 0;
        for (Long l : readnames.keySet()) {
            ids[i++] = l.longValue();
        }
        
        //System.err.print("will fetch "+ids.length+ " entries for this chunk.");

        try {
            for (DNASequenceHolder holder : reader.fetch(ids)) {
                DNASequenceI seq = holder.getSequence();
                SequenceDTO dto = SequenceDTO.newBuilder()
                        .setId(seq.getId())
                        .setName(readnames.remove(seq.getId()))
                        .setSequence(new String(seq.getSequence()))
                        .build();
                listBuilder.addSeq(dto);
            }
        } catch (SeqStoreException ex) {
            throw new MGXException(ex);
        }
        lastAccessed = System.currentTimeMillis();
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
        } catch (SQLException ex) {
        }
        super.close();
    }

    @Override
    public boolean isFinished() {
        lastAccessed = System.currentTimeMillis();
        return have_more_data;
    }


    private static String buildSQLTemplate(int numElements) {
        StringBuilder sb = new StringBuilder(GET_SEQS);
        for (int i = 1; i < numElements; i++) {
            sb.append(",?");
        }
        sb.append(") ORDER BY r.id ASC");
        return sb.toString();
    }
}