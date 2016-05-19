package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Observation;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.misc.SequenceObservation;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class ObservationDAO<T extends Observation> {

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

    public DBIterator<SequenceObservation> byRead(long seqId) throws MGXException {
        DBIterator<SequenceObservation> iter = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;
        Connection conn = null;

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
