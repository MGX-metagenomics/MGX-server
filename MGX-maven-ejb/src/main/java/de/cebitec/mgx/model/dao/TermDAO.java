package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Term;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public DBIterator<Term> byCategory(String cat) {
        DBIterator<Term> iter = null;
        try {
            Connection conn = globalDS.getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_CATNAME);
            stmt.setString(1, cat);
            ResultSet rset = stmt.executeQuery();

            iter = new DBIterator<Term>(rset, stmt, conn) {
                @Override
                public Term convert(ResultSet rs) throws SQLException {
                    Term t = new Term();
                    t.setId(rs.getLong(1));
                    t.setParentId(rs.getLong(2));
                    t.setName(rs.getString(3));
                    t.setDescription(rs.getString(4));
                    return t;
                }
            };

        } catch (SQLException ex) {
            Logger.getLogger(TermDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return iter;
    }
}
