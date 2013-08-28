package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sj
 */
public class ReferenceDAO<T extends Reference> extends DAO<T> {

    @Override
    public Class getType() {
        return Reference.class;
    }

    public DBIterator<Region> byReferenceInterval(long seqId, int from, int to) throws MGXException {
        DBIterator<Region> iter = null;
        Connection conn = getConnection();
        PreparedStatement stmt = null;
        ResultSet rset = null;
        try {
            stmt = conn.prepareStatement("SELECT * from getRegions(?,?,?)");
            stmt.setLong(1, seqId);
            stmt.setInt(2, from);
            stmt.setInt(3, to);
            rset = stmt.executeQuery();

            iter = new DBIterator<Region>(rset, stmt, conn) {
                @Override
                public Region convert(ResultSet rs) throws SQLException {
                    Region r = new Region();
                    r.setId(rs.getLong(1));
                    r.setName(rs.getString(2));
                    r.setDescription(rs.getString(3));
                    r.setStart(rs.getInt(4));
                    r.setStop(rs.getInt(5));
                    return r;
                }
            };

        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }
}
