package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.misc.BulkObservation;
import de.cebitec.mgx.model.misc.SequenceObservation;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class ObservationDAO {

    private final MGXController ctx;

    public ObservationDAO(MGXController ctx) {
        this.ctx = ctx;
    }

    public void create(SequenceObservation obs, Sequence seq, Attribute attr) throws MGXException {
        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO observation (start, stop, seq_id, attr_id) VALUES (?,?,?,?)")) {
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

    public void create(long seqId, long attrId, int start, int stop) throws MGXException {
        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO observation (start, stop, seq_id, attr_id) VALUES (?,?,?,?)")) {
                stmt.setInt(1, start);
                stmt.setInt(2, stop);
                stmt.setLong(3, seqId);
                stmt.setLong(4, attrId);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    public void delete(long seqId, long attrId, int start, int stop) throws MGXException {
        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE seq_id=? AND attr_id=? AND start=? AND stop=?")) {
                stmt.setLong(1, seqId);
                stmt.setLong(2, attrId);
                stmt.setInt(3, start);
                stmt.setInt(4, stop);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    private final static String SQL_BULK_OBS
            = "INSERT INTO observation (start, stop, attr_id, seq_id) SELECT ?, ?, ?, id FROM read WHERE seqrun_id=? AND name=?";

    public void createBulk(List<BulkObservation> data) throws MGXException {

        try (Connection conn = ctx.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BULK_OBS)) {
                for (BulkObservation obs : data) {
                    stmt.setInt(1, obs.getStart());
                    stmt.setInt(2, obs.getStop());
                    stmt.setLong(3, obs.getAttributeId());
                    stmt.setLong(4, obs.getSeqRunId());
                    stmt.setString(5, obs.getSequenceName());
                    stmt.addBatch();
                }
                int[] status = stmt.executeBatch();

                if (status == null || status.length != data.size()) {
                    throw new MGXException("Database batch update failed. Expected " + data.size()
                            + ", got " + (status == null ? "null" : String.valueOf(status.length)));
                }

                // check number of affected rows for each batch; as each batch entry
                // represents a single new observation to add, we expect int[]{1,1,1..}
                int idx = 0;
                for (int i : status) {
                    if (i != 1) {
                        throw new MGXException("Database batch updated failed. First failing read was " + data.get(idx).getSequenceName()
                                + " with status " + String.valueOf(i) + ".");
                    }
                    idx++;
                }

            }
        } catch (SQLException ex) {
            ctx.log(ex.getMessage());
            while (ex.getNextException() != null) {
                ex = ex.getNextException();
                ctx.log(ex.getMessage());
            }
            throw new MGXException(ex.getMessage());
        }
    }

    public DBIterator<SequenceObservation> byRead(final long seqId) throws MGXException {
        if (seqId <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        DBIterator<SequenceObservation> iter = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;
        Connection conn = null;

        //
        // no try-with-resources here; the DBIterator will take care of
        // closing the database resources
        //
        try {
            conn = ctx.getConnection();
            stmt = conn.prepareStatement("SELECT * from getObservations(?)");
            stmt.setLong(1, seqId);
            rset = stmt.executeQuery();

            iter = new DBIterator<SequenceObservation>(rset, stmt, conn) {
                @Override
                public SequenceObservation convert(ResultSet rs) throws SQLException {
                    return new SequenceObservation(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4));
                }
            };

        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

}
