package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.ReferenceRegion;
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
public class ReferenceRegionDAO extends DAO<ReferenceRegion> {

    public ReferenceRegionDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<ReferenceRegion> getType() {
        return ReferenceRegion.class;
    }

    private final static String CREATE = "INSERT INTO region (name, description, type, reg_start, reg_stop, ref_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(ReferenceRegion obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getDescription());
                stmt.setString(3, obj.getType().toString());
                stmt.setInt(4, obj.getStart());
                stmt.setInt(5, obj.getStop());
                stmt.setLong(6, obj.getReferenceId());

                try ( ResultSet rs = stmt.executeQuery()) {
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
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM region WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type ReferenceRegion for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    private static final String BY_ID = "SELECT id, name, description, type, reg_start, reg_stop, ref_id FROM region "
            + "WHERE id=?";

    @Override
    public Result<ReferenceRegion> getById(final long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type ReferenceRegion for ID " + id + ".");
                    }
                    ReferenceRegion ret = new ReferenceRegion();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setDescription(rs.getString(3));

                    String type = rs.getString(4);
                    switch (type) {
                        case "CDS":
                            ret.setType(RegionType.CDS);
                            break;
                        case "tRNA":
                            ret.setType(RegionType.TRNA);
                            break;
                        case "rRNA":
                            ret.setType(RegionType.RRNA);
                            break;
                        case "tmRNA":
                            ret.setType(RegionType.TMRNA);
                            break;
                        case "ncRNA":
                            ret.setType(RegionType.NCRNA);
                            break;
                        default:
                            getController().log("Unhandled region type " + type + "for ID " + ret.getId());
                            ret.setType(RegionType.MISC);

                    }

                    ret.setStart(rs.getInt(5));
                    ret.setStop(rs.getInt(6));
                    ret.setReferenceId(rs.getLong(7));

                    return Result.ok(ret);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private static final String REGIONS_BY_REF = "SELECT id, name, description, type, reg_start, reg_stop FROM region "
            + "WHERE ref_id=?";

    public Result<AutoCloseableIterator<ReferenceRegion>> byReference(final long ref_id) {
        if (ref_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(REGIONS_BY_REF);
            stmt.setLong(1, ref_id);
            ResultSet rs = stmt.executeQuery();

            DBIterator<ReferenceRegion> dbIterator = new DBIterator<ReferenceRegion>(rs, stmt, conn) {
                @Override
                public ReferenceRegion convert(ResultSet rs) throws SQLException {
                    ReferenceRegion ret = new ReferenceRegion();
                    ret.setReferenceId(ref_id);
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setDescription(rs.getString(3));

                    String type = rs.getString(4);
                    switch (type) {
                        case "CDS":
                            ret.setType(RegionType.CDS);
                            break;
                        case "tRNA":
                            ret.setType(RegionType.TRNA);
                            break;
                        case "rRNA":
                            ret.setType(RegionType.RRNA);
                            break;
                        case "tmRNA":
                            ret.setType(RegionType.TMRNA);
                            break;
                        case "ncRNA":
                            ret.setType(RegionType.NCRNA);
                            break;
                        default:
                            getController().log("Unhandled region type " + type + "for ID " + ret.getId());
                            ret.setType(RegionType.MISC);

                    }

                    ret.setStart(rs.getInt(5));
                    ret.setStop(rs.getInt(6));
                    return ret;
                }
            };

            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<AutoCloseableIterator<ReferenceRegion>> byReferenceInterval(final long refId, final Reference ref, int from, int to) {

        if (from > to || from < 0 || to < 0 || from == to || to > ref.getLength()) {
            return Result.error("Invalid coordinates: " + from + " " + to);
        }
        DBIterator<ReferenceRegion> iter = null;
        ResultSet rset;
        PreparedStatement stmt;

        try {
            Connection conn = getConnection();
            stmt = conn.prepareStatement("SELECT * from getRegions(?,?,?)");
            stmt.setLong(1, refId);
            stmt.setInt(2, from);
            stmt.setInt(3, to);
            rset = stmt.executeQuery();

            iter = new DBIterator<ReferenceRegion>(rset, stmt, conn) {
                @Override
                public ReferenceRegion convert(ResultSet rs) throws SQLException {
                    ReferenceRegion r = new ReferenceRegion();
                    r.setReferenceId(refId);
                    r.setId(rs.getLong(1));
                    r.setName(rs.getString(2));
                    String type = rs.getString(3);
                    switch (type) {
                        case "CDS":
                            r.setType(RegionType.CDS);
                            break;
                        case "tRNA":
                            r.setType(RegionType.TRNA);
                            break;
                        case "rRNA":
                            r.setType(RegionType.RRNA);
                            break;
                        case "tmRNA":
                            r.setType(RegionType.TMRNA);
                            break;
                        case "ncRNA":
                            r.setType(RegionType.NCRNA);
                            break;
                        default:
                            getController().log("Unhandled region type " + type + "for ID " + r.getId());
                            r.setType(RegionType.MISC);

                    }

                    r.setDescription(rs.getString(4));
                    r.setStart(rs.getInt(5));
                    r.setStop(rs.getInt(6));
                    return r;
                }
            };

            return Result.ok(iter);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }
}
