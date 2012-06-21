package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Term;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class TermDAO<T extends Term> extends DAO<T> {

    private DataSource globalDS;
    private final static String BY_CATNAME = "SELECT t.id, t.parent_id, t.name, "
            + "t.description FROM term t LEFT JOIN category ON (t.cat_id = category.id) "
            + "WHERE category.name=?";

    public TermDAO(DataSource ds) {
        globalDS = ds;
    }

    @Override
    Class getType() {
        return Term.class;
    }

    public Collection<Term> byCategory(String cat) {
        List<Term> ret = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = globalDS.getConnection();
            stmt = conn.prepareStatement(BY_CATNAME);
            stmt.setString(1, cat);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Term t = new Term();
                t.setId(rs.getLong(1));
                t.setParentId(rs.getLong(2));
                t.setName(rs.getString(3));
                t.setDescription(rs.getString(4));
                ret.add(t);
            }
        } catch (SQLException ex) {
            Logger.getLogger(TermDAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            close(conn, stmt, rs);
        }
        return ret;
    }
}
