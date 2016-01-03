package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Observation;
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

    public DBIterator<SequenceObservation> byRead(long seqId) throws MGXException {
        DBIterator<SequenceObservation> iter = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;

        try (Connection conn = ctx.getConnection()) {
            stmt = conn.prepareStatement("SELECT * from getObservations(?)");
            stmt.setLong(1, seqId);
            rset = stmt.executeQuery();

            iter = new DBIterator<SequenceObservation>(rset, stmt, conn) {
                @Override
                public SequenceObservation convert(ResultSet rs) throws SQLException {
                    SequenceObservation obs = new SequenceObservation();
                    obs.setStart(rs.getInt(1));
                    obs.setStop(rs.getInt(2));
                    obs.setAttributeName(rs.getString(3));
                    obs.setAttributeTypeName(rs.getString(4));
                    return obs;
                }
            };

        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

    public MGXController getController() {
        return ctx;
    }

//    public Connection getConnection() throws SQLException {
//        return ctx.getConnection();
//    }
}