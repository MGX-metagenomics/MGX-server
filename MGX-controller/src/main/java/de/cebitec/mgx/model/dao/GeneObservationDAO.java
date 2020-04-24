package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.model.misc.GeneObservation;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class GeneObservationDAO {

    private final MGXController ctx;

    public GeneObservationDAO(MGXController ctx) {
        this.ctx = ctx;
    }

    public void create(GeneObservation obs, Gene seq, Attribute attr) throws MGXException {
        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO gene_observation (start, stop, gene_id, attr_id) VALUES (?,?,?,?)")) {
                stmt.setInt(1, obs.getStart());
                stmt.setInt(2, obs.getStop());
                stmt.setLong(3, seq.getId());
                stmt.setLong(4, attr.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    public void create(long geneId, long attrId, int start, int stop) throws MGXException {
        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO gene_observation (start, stop, gene_id, attr_id) VALUES (?,?,?,?)")) {
                stmt.setInt(1, start);
                stmt.setInt(2, stop);
                stmt.setLong(3, geneId);
                stmt.setLong(4, attrId);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

//    public void delete(long seqId, long attrId, int start, int stop) throws MGXException {
//        try (Connection conn = ctx.getConnection()) {
//            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE seq_id=? AND attr_id=? AND start=? AND stop=?")) {
//                stmt.setLong(1, seqId);
//                stmt.setLong(2, attrId);
//                stmt.setInt(3, start);
//                stmt.setInt(4, stop);
//                stmt.executeUpdate();
//            }
//        } catch (SQLException ex) {
//            throw new MGXException(ex.getMessage());
//        }
//    }
//
//    private final static String SQL_BULK_OBS
//            = "INSERT INTO observation (start, stop, attr_id, seq_id) SELECT ?, ?, ?, id FROM read WHERE seqrun_id=? AND name=?";
//
//    public void createBulk(List<BulkObservation> data) throws MGXException {
//
//        try (Connection conn = ctx.getConnection()) {
//            try (PreparedStatement stmt = conn.prepareStatement(SQL_BULK_OBS)) {
//                for (BulkObservation obs : data) {
//                    stmt.setInt(1, obs.getStart());
//                    stmt.setInt(2, obs.getStop());
//                    stmt.setLong(3, obs.getAttributeId());
//                    stmt.setLong(4, obs.getSeqRunId());
//                    stmt.setString(5, obs.getSequenceName());
//                    stmt.addBatch();
//                }
//                int[] status = stmt.executeBatch();
//
//                if (status == null || status.length != data.size()) {
//                    throw new MGXException("Database batch update failed. Expected " + data.size()
//                            + ", got " + (status == null ? "null" : String.valueOf(status.length)));
//                }
//
//                // check number of affected rows for each batch; as each batch entry
//                // represents a single new observation to add, we expect int[]{1,1,1..}
//                int idx = 0;
//                for (int i : status) {
//                    if (i != 1) {
//                        throw new MGXException("Database batch updated failed. First failing read was " + data.get(idx).getSequenceName()
//                                + " with status " + String.valueOf(i) + ".");
//                    }
//                    idx++;
//                }
//
//            }
//        } catch (SQLException ex) {
//            ctx.log(ex);
//            SQLException sqle = ex;
//            while (sqle.getNextException() != null) {
//                sqle = sqle.getNextException();
//                ctx.log(sqle);
//            }
//            throw new MGXException(ex.getMessage());
//        }
//    }

    public DBIterator<GeneObservation> byRead(final long geneId) throws MGXException {
        if (geneId <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        DBIterator<GeneObservation> iter = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;
        Connection conn = null;

        //
        // no try-with-resources here; the DBIterator will take care of
        // closing the database resources
        //
        try {
            conn = ctx.getConnection();
            stmt = conn.prepareStatement("SELECT * from getGeneObservations(?)");
            stmt.setLong(1, geneId);
            rset = stmt.executeQuery();

            iter = new DBIterator<GeneObservation>(rset, stmt, conn) {
                @Override
                public GeneObservation convert(ResultSet rs) throws SQLException {
                    return new GeneObservation(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));
                }
            };

        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

}
