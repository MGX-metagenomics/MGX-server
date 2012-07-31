package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Observation;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.util.SequenceObservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class ObservationDAO<T extends Observation> {

    public List<SequenceObservation> byRead(long seqId) throws MGXException {
        List<SequenceObservation> ret = new ArrayList<>();
        Connection conn = getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * from getObservations(?)");
            stmt.setLong(1, seqId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                SequenceObservation obs = new SequenceObservation();
                obs.setStart(rs.getInt(1));
                obs.setStop(rs.getInt(2));
                obs.setAttributeName(rs.getString(3));
                obs.setAttributeTypeName(rs.getString(4));
                ret.add(obs);
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, rs);
        }
        return ret;
    }
    
    private MGXController ctx;

    public void setController(MGXController ctx) {
        this.ctx = ctx;
    }

    public MGXController getController() {
        return ctx;
    }

    public Connection getConnection() {
        return ctx.getConnection();
    }

    protected void close(Connection c, Statement s, ResultSet r) {
        try {
            if (r != null) {
                r.close();
            }
            if (s != null) {
                s.close();
            }
            if (c != null) {
                c.close();
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (s != null) {
                    s.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException ex) {
            }
        }
    }
}
