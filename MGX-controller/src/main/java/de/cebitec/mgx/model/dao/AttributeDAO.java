package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.Pair;
import de.cebitec.mgx.util.Triple;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author sjaenick
 */
public class AttributeDAO extends DAO<Attribute> {

    public AttributeDAO(MGXController ctx) {
        super(ctx);
    }

    private final static String CREATE = "INSERT INTO attribute (value, attrtype_id, job_id, parent_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Attribute obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getValue());
                stmt.setLong(2, obj.getAttributeTypeId());
                stmt.setLong(3, obj.getJobId());
                if (obj.getParentId() != Attribute.INVALID_IDENTIFIER) {
                    stmt.setLong(4, obj.getParentId());
                } else {
                    stmt.setNull(4, java.sql.Types.BIGINT);
                }

                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    public void delete(long id) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Attribute for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private final static String BY_ID = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id, attr.job_id FROM attribute attr "
            + "WHERE id=?";

    @Override
    public Result<Attribute> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);

                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type Attribute for ID " + id + ".");
                    }
                    Attribute attr = new Attribute();
                    attr.setId(rs.getLong(1));
                    attr.setValue(rs.getString(2));
                    attr.setAttributeTypeId(rs.getLong(3));
                    long parentId = rs.getLong(4);
                    if (parentId != 0) {
                        attr.setParentId(parentId);
                    }
                    attr.setJobId(rs.getLong(5));

                    return Result.ok(attr);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<AutoCloseableIterator<Attribute>> getByIds(long... ids) {
        if (ids == null || ids.length == 0) {
            return Result.error("Null/empty ID list.");
        }

        List<Attribute> ret = null;

        String BY_IDS = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id, attr.job_id FROM attribute attr "
                + "WHERE id IN (" + toSQLTemplateString(ids.length) + ")";

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_IDS)) {
                int idx = 1;
                for (Long id : ids) {
                    stmt.setLong(idx++, id);
                }

                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setId(rs.getLong(1));
                        attr.setValue(rs.getString(2));
                        attr.setAttributeTypeId(rs.getLong(3));
                        long parentId = rs.getLong(4);
                        if (parentId != 0) {
                            attr.setParentId(parentId);
                        }
                        attr.setJobId(rs.getLong(5));

                        if (ret == null) {
                            ret = new ArrayList<>(ids.length);
                        }

                        ret.add(attr);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
            return Result.error(ex.getMessage());
        }
        return Result.ok(new ForwardingIterator<>(ret == null ? null : ret.iterator()));
    }

    @Override
    Class<Attribute> getType() {
        return Attribute.class;
    }

    public Result<List<Triple<Attribute, Long, Long>>> getDistribution(long attrTypeId, long jobId, long seqrunId) {

        // attribute, parent id, count
        List<Triple<Attribute, Long, Long>> ret = new LinkedList<>();

        try ( Connection conn = getController().getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getDistribution(?,?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, jobId);
                stmt.setLong(3, seqrunId);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setAttributeTypeId(attrTypeId);
                        attr.setJobId(jobId);
                        //
                        attr.setId(rs.getLong(1));
                        attr.setValue(rs.getString(2));

                        Long count = rs.getLong(3);
                        // 
                        // read the parent attributes id, if present
                        Long parentId = rs.getLong(4);
                        if (parentId == 0) {
                            parentId = -1L;
                        }

                        ret.add(new Triple<>(attr, parentId, count));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        return Result.ok(ret);

    }

    public Result<List<Triple<Attribute, Long, Long>>> getFilteredDistribution(long filterAttrId, long attrTypeId, long jobId) {

        // attribute, parent id, count
        List<Triple<Attribute, Long, Long>> ret = new LinkedList<>();

        try ( Connection conn = getController().getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getFilteredDistribution(?,?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, jobId);
                stmt.setLong(3, filterAttrId);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setAttributeTypeId(attrTypeId);
                        attr.setJobId(jobId);
                        //
                        attr.setId(rs.getLong(1));
                        attr.setValue(rs.getString(2));

                        Long count = rs.getLong(3);
                        // 
                        // read the parent attributes id, if present
                        Long parentId = rs.getLong(4);
                        if (parentId == 0) {
                            parentId = -1L;
                        }

                        ret.add(new Triple<>(attr, parentId, count));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        return Result.ok(ret);

    }

    public Result<Map<Attribute, Long>> getHierarchy(long attrTypeId, long job_id, long seqrunId) {

        Map<Attribute, Long> attrCount = new HashMap<>();
        TLongObjectMap<Attribute> attrByID = new TLongObjectHashMap<>();
        TLongLongMap attr2parent = new TLongLongHashMap();

        try ( Connection conn = getController().getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getHierarchy(?,?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, job_id);
                stmt.setLong(3, seqrunId);

                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        //  attrtype_id | attrtype_name | atype_structure | attrtype_valtype | attr_id | attr_value | parent_id | count
                        long aTypeID = rs.getLong(1);

                        Attribute attr = new Attribute();
                        attr.setAttributeTypeId(aTypeID);
                        attr.setJobId(job_id);
                        attr.setId(rs.getLong(5));
                        attr.setValue(rs.getString(6));

                        long parentId = rs.getLong(7);
                        attr2parent.put(attr.getId(), parentId);
                        attrByID.put(attr.getId(), attr);

                        attrCount.put(attr, rs.getLong(8));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        for (Attribute a : attrCount.keySet()) {
            long parentID = attr2parent.get(a.getId());
            if (parentID != 0) {
                Attribute parent = attrByID.get(parentID);
                a.setParentId(parent.getId());
            }
        }

        return Result.ok(attrCount);
    }

    public Result<Map<Pair<Attribute, Attribute>, Integer>> getCorrelation(long attrTypeId, long job1Id, long attrType2Id, long job2id) {

        Map<Pair<Attribute, Attribute>, Integer> ret = new HashMap<>();

        try ( Connection conn = getController().getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getCorrelation(?,?,?,?)")) {
                stmt.setLong(1, job1Id);
                stmt.setLong(2, attrTypeId);
                stmt.setLong(3, job2id);
                stmt.setLong(4, attrType2Id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setAttributeTypeId(attrTypeId);
                        attr.setJobId(job1Id);
                        //
                        attr.setId(rs.getLong(1));
                        attr.setValue(rs.getString(2));

                        Attribute attr2 = new Attribute();
                        attr2.setAttributeTypeId(attrType2Id);
                        attr2.setJobId(job2id);
                        //
                        attr2.setId(rs.getLong(3));
                        attr2.setValue(rs.getString(4));

                        int cnt = rs.getInt(5);

                        ret.put(new Pair<>(attr, attr2), cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(ret);
    }

    private final static String SQL_BYJOB = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE job_id=? AND job.job_state=?";

    public Result<AutoCloseableIterator<Attribute>> byJob(long jobId) {

        Result<DBIterator<AttributeType>> res = getController().getAttributeTypeDAO().byJob(jobId);
        if (res.isError()) {
            return Result.error(res.getError());
        }

        // pre-collect attribute types
        final TLongObjectMap<AttributeType> attrTypes = new TLongObjectHashMap<>();
        try ( DBIterator<AttributeType> aTypes = res.getValue()) {
            while (aTypes.hasNext()) {
                AttributeType attrType = aTypes.next();
                attrTypes.put(attrType.getId(), attrType);
            }
        }

        List<Attribute> attrs = new ArrayList<>();

        try ( Connection conn = getController().getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BYJOB)) {
                stmt.setLong(1, jobId);
                stmt.setInt(2, JobState.FINISHED.getValue());
                try ( ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setId(rs.getLong(1));
                        attr.setJobId(jobId);
                        attr.setValue(rs.getString(2));

                        AttributeType aType = attrTypes.get(rs.getLong(3));
                        attr.setAttributeTypeId(aType.getId());

                        // fetch parent, if present
                        long parentId = rs.getLong(4);
                        if (parentId != 0) {
                            //Attribute parent = getController().getAttributeDAO().getById(parentId);
                            attr.setParentId(parentId);
                        }

                        attrs.add(attr);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        attrTypes.clear();

        return Result.ok(new ForwardingIterator<>(attrs.iterator()));
    }

    public Result<AutoCloseableIterator<Attribute>> bySeqRun(long runId) {

        Result<AutoCloseableIterator<Job>> res = getController().getJobDAO().bySeqRun(runId);
        if (res.isError()) {
            return Result.error(res.getError());
        }

        List<Attribute> attrs = new ArrayList<>();

        try ( AutoCloseableIterator<Job> jobIter = res.getValue()) {
            while (jobIter.hasNext()) {
                Job job = jobIter.next();

                Result<AutoCloseableIterator<Attribute>> res2 = getController().getAttributeDAO().byJob(job.getId());
                if (res2.isError()) {
                    return Result.error(res2.getError());
                }
                try ( AutoCloseableIterator<Attribute> attrIter = res2.getValue()) {
                    while (attrIter.hasNext()) {
                        attrs.add(attrIter.next());
                    }
                }
            }
        }

        return Result.ok(new ForwardingIterator<>(attrs.iterator()));
    }

    private final static String SQL_FIND = "SELECT value FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE ?=ANY(job.seqruns) AND job.job_state=? "
            + "AND upper(attr.value) LIKE CONCAT('%', upper(?), '%')";

    public Result<DBIterator<String>> find(String term, long runId) {
        if (term == null || term.isEmpty()) {
            return Result.error("Empty search term.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_FIND);
            stmt.setLong(1, runId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            stmt.setString(3, term);
            ResultSet rset = stmt.executeQuery();

            DBIterator<String> iter = new DBIterator<String>(rset, stmt, conn) {
                @Override
                public String convert(ResultSet rs) throws SQLException {
                    return rs.getString(1);
                }
            };
            return Result.ok(iter);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String SQL_SEARCH = "WITH matching_attrs AS ( "
            + "SELECT attr.id FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE ?=ANY(job.seqruns) AND job.job_state=? "
            + "AND (? AND upper(attr.value) = upper(?) "
            + "OR NOT (?) AND upper(attr.value) LIKE CONCAT('%', upper(?), '%')) "
            + ")"
            + "SELECT DISTINCT read.id AS read_id, read.name AS read_name, read.length as read_length "
            + "FROM read "
            + "JOIN observation obs ON (read.id = obs.seq_id) "
            + "JOIN matching_attrs ON (obs.attr_id = matching_attrs.id)";

    public Result<DBIterator<Sequence>> search(String term, boolean exact, long runId) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_SEARCH);
            stmt.setLong(1, runId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            stmt.setBoolean(3, exact);
            stmt.setString(4, term);
            stmt.setBoolean(5, exact);
            stmt.setString(6, term);

            ResultSet rset = stmt.executeQuery();

            DBIterator<Sequence> dbIterator = new DBIterator<Sequence>(rset, stmt, conn) {
                @Override
                public Sequence convert(ResultSet rs) throws SQLException {
                    Sequence s = new Sequence();
                    s.setId(rs.getLong(1));
                    s.setName(rs.getString(2));
                    s.setLength(rs.getInt(3));
                    return s;
                }
            };
            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

}
