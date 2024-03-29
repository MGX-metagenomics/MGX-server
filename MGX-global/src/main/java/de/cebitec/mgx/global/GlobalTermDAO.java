package de.cebitec.mgx.global;

import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.global.model.Term;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class GlobalTermDAO {

    private final MGXGlobal global;
    private final static String BY_CATNAME = "SELECT t.id, t.parent_id, t.name, "
            + "t.description FROM term t LEFT JOIN category ON (t.cat_id = category.id) "
            + "WHERE category.name=?";

    public GlobalTermDAO(final MGXGlobal global) {
        this.global = global;
    }

    public Result<Term> getById(long id) {
        Term t = null;
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT parent_id, name, description FROM term WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        t = new Term();
                        t.setId(id);
                        t.setParentId(rs.getLong(1));
                        t.setName(rs.getString(2));
                        t.setDescription(rs.getString(3));
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalTermDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(t);
    }

    public Result<AutoCloseableIterator<Term>> byCategory(String cat) {
        List<Term> terms = new ArrayList<>();
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_CATNAME)) {
                stmt.setString(1, cat);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Term t = new Term();
                        t.setId(rs.getLong(1));
                        t.setParentId(rs.getLong(2));
                        t.setName(rs.getString(3));
                        t.setDescription(rs.getString(4));
                        terms.add(t);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalTermDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(new ForwardingIterator<>(terms.iterator()));
    }
}
