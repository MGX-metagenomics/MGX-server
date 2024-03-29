package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.util.AutoCloseableIterator;
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
public class AttributeTypeDAO extends DAO<AttributeType> {

    public AttributeTypeDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<AttributeType> getType() {
        return AttributeType.class;
    }

    private final static String CREATE = "INSERT INTO attributetype (name, structure, value_type) "
            + "VALUES (?,?,?) RETURNING id";

    @Override
    public long create(AttributeType obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, String.valueOf(obj.getStructure()));
                stmt.setString(3, String.valueOf(obj.getValueType()));

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
            try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributetype WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type AttributeType for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    private final static String BY_ID = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type, atype.structure "
            + "FROM attributetype atype WHERE id=?";

    @Override
    public Result<AttributeType> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);

                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type AttributeType for ID " + id + ".");
                    }
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));

                    return Result.ok(aType);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<AutoCloseableIterator<AttributeType>> getByIds(long... ids) {
        if (ids == null || ids.length == 0) {
            return Result.error("Null/empty ID list.");
        }
        List<AttributeType> ret = null;

        String BY_IDS = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type, atype.structure "
                + "FROM attributetype atype WHERE id IN (" + toSQLTemplateString(ids.length) + ")";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_IDS);
            int idx = 1;
            for (Long id : ids) {
                stmt.setLong(idx++, id);
            }
            ResultSet rs = stmt.executeQuery();

            DBIterator<AttributeType> dbIterator = new DBIterator<AttributeType>(rs, stmt, conn) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex.getMessage());
            return Result.error(ex.getMessage());
        }
    }

    private final static String FETCHALL = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type, atype.structure "
            + "FROM attributetype atype "
            + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE job.job_state=? "
            + "GROUP BY atype.id, atype.name, atype.value_type, atype.structure "
            + "ORDER BY atype.name ASC";

    public Result<AutoCloseableIterator<AttributeType>> getAll() {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            stmt.setInt(1, JobState.FINISHED.getValue());
            ResultSet rs = stmt.executeQuery();

            DBIterator<AttributeType> dbIterator = new DBIterator<AttributeType>(rs, stmt, conn) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };
            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    /*
     *  returns attribute types for a job, if it's in FINISHED state
     */
    public Result<DBIterator<AttributeType>> byJob(long jobId) {

        final String sql = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type, atype.structure "
                + "FROM attributetype atype "
                + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE job.id=? AND job.job_state=? "
                + "GROUP BY atype.id, atype.name, atype.value_type, atype.structure "
                + "ORDER BY atype.name ASC";

        PreparedStatement stmt;
        ResultSet rset;
        try {
            Connection c = getConnection();
            stmt = c.prepareStatement(sql);
            stmt.setLong(1, jobId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            rset = stmt.executeQuery();

            DBIterator<AttributeType> dbIterator = new DBIterator<AttributeType>(rset, stmt, c) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };
            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<DBIterator<AttributeType>> BySeqRun(long seqrunId) {

        DBIterator<AttributeType> iter = null;

        final String sql = "SELECT DISTINCT atype.id, atype.name, atype.value_type, atype.structure "
                + "FROM job "
                + "JOIN attribute attr ON (job.id = attr.job_id) "
                + "JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "WHERE job.job_state=? AND ?=ANY(job.seqruns)";

        PreparedStatement stmt;
        ResultSet rset;
        try {
            Connection c = getConnection();
            stmt = c.prepareStatement(sql);
            stmt.setInt(1, JobState.FINISHED.getValue());
            stmt.setLong(2, seqrunId);
            rset = stmt.executeQuery();

            iter = new DBIterator<AttributeType>(rset, stmt, c) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };
            return Result.ok(iter);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }
}
