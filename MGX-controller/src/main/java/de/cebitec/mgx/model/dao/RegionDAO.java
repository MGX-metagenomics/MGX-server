package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author belmann
 */
public class RegionDAO extends DAO<Region> {

    public RegionDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Region.class;
    }

    private final static String CREATE = "INSERT INTO region (name, description, type, reg_start, reg_stop, ref_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Region obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getDescription());
                stmt.setString(3, obj.getType());
                stmt.setInt(4, obj.getStart());
                stmt.setInt(5, obj.getStop());
                stmt.setLong(6, obj.getReferenceId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    public void delete(long id) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    private static final String BY_ID = "SELECT id, name, description, type, reg_start, reg_stop, ref_id FROM region "
            + "WHERE id=?";

    @Override
    public Region getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }
                    Region ret = new Region();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setDescription(rs.getString(3));
                    ret.setType(rs.getString(4));
                    ret.setStart(rs.getInt(5));
                    ret.setStop(rs.getInt(6));
                    ret.setReferenceId(rs.getLong(7));

                    return ret;
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private static final String REGIONS_BY_REF = "SELECT id, name, description, type, reg_start, reg_stop FROM region "
            + "WHERE ref_id=?";

    public AutoCloseableIterator<Region> byReference(final Reference ref) throws MGXException {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(REGIONS_BY_REF);
            stmt.setLong(1, ref.getId());
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<Region>(rs, stmt, conn) {
                @Override
                public Region convert(ResultSet rs) throws SQLException {
                    Region ret = new Region();
                    ret.setReferenceId(ref.getId());
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setDescription(rs.getString(3));
                    ret.setType(rs.getString(4));
                    ret.setStart(rs.getInt(5));
                    ret.setStop(rs.getInt(6));
                    return ret;
                }
            };
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

//    public AutoCloseableIterator<Region> byReference(Reference s) throws MGXException {
//        Iterator<Region> iterator = getEntityManager().createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.reference = :reference", Region.class).
//                setParameter("reference", s).getResultList().iterator();
//        return new ForwardingIterator<>(iterator);
//    }
}
