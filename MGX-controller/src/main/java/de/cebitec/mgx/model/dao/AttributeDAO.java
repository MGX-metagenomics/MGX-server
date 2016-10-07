package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
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
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getValue());
                stmt.setLong(2, obj.getAttributeTypeId());
                stmt.setLong(3, obj.getJobId());
                if (obj.getParentId() != Attribute.INVALID_IDENTIFIER) {
                    stmt.setLong(4, obj.getParentId());
                } else {
                    stmt.setNull(4, java.sql.Types.BIGINT);
                }

                try (ResultSet rs = stmt.executeQuery()) {
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
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
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
    public Attribute getById(long id) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
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

                    return attr;
                }
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
            throw new MGXException(ex);
        }
    }

    public AutoCloseableIterator<Attribute> getByIds(long... ids) throws MGXException {
        if (ids == null || ids.length == 0) {
            throw new MGXException("Null/empty ID list.");
        }

        List<Attribute> ret = null;

        String BY_IDS = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id, attr.job_id FROM attribute attr "
                + "WHERE id IN (" + toSQLTemplateString(ids.length) + ")";

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_IDS)) {
                int idx = 1;
                for (Long id : ids) {
                    stmt.setLong(idx++, id);
                }

                try (ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    @Override
    Class<Attribute> getType() {
        return Attribute.class;
    }

    public List<Triple<Attribute, Long, Long>> getDistribution(long attrTypeId, long jobId) throws MGXException {

//        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);
//        Job job = getController().getJobDAO().getById(jobId);

        // attribute, parent id, count
        List<Triple<Attribute, Long, Long>> ret = new LinkedList<>();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getDistribution(?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, jobId);
                try (ResultSet rs = stmt.executeQuery()) {
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
            getController().log(ex.getMessage());
            throw new MGXException(ex.getMessage());
        }

        return ret;
    }

    public Map<Attribute, Long> getHierarchy(long attrTypeId, long job_id) throws MGXException {

        Map<Attribute, Long> ret = new HashMap<>();
//        TLongObjectMap<AttributeType> aTypeCache = new TLongObjectHashMap<>();
        TLongObjectMap<Attribute> attrCache = new TLongObjectHashMap<>();
        TLongLongMap attr2parent = new TLongLongHashMap();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getHierarchy(?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, job_id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        //  attrtype_id | attrtype_name | atype_structure | attrtype_valtype | attr_id | attr_value | parent_id | count
//                        AttributeType aType;
                        long aTypeID = rs.getLong(1);
//                        if (aTypeCache.containsKey(aTypeID)) {
//                            aType = aTypeCache.get(aTypeID);
//                        } else {
//                            aType = new AttributeType();
//                            aType.setId(aTypeID);
//                            aType.setName(rs.getString(2));
//                            aType.setStructure(rs.getString(3).charAt(0));
//                            aType.setValueType(rs.getString(4).charAt(0));
//                            aTypeCache.put(aTypeID, aType);
//                        }

                        Attribute attr = new Attribute();
                        attr.setAttributeTypeId(aTypeID);
                        attr.setJobId(job_id);
                        attr.setId(rs.getLong(5));
                        attr.setValue(rs.getString(6));

                        long parentId = rs.getLong(7);
                        attr2parent.put(attr.getId(), parentId);
                        attrCache.put(attr.getId(), attr);

                        ret.put(attr, rs.getLong(8));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
            throw new MGXException(ex.getMessage());
        }

        for (Attribute a : ret.keySet()) {
            long parentID = attr2parent.get(a.getId());
            if (parentID != 0) {
                Attribute parent = attrCache.get(parentID);
                a.setParentId(parent.getId());
            }
        }

        return ret;
    }

    public Map<Pair<Attribute, Attribute>, Integer> getCorrelation(long attrTypeId, long job1Id, long attrType2Id, long job2id) throws MGXException {

        Map<Pair<Attribute, Attribute>, Integer> ret = new HashMap<>();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getCorrelation(?,?,?,?)")) {
                stmt.setLong(1, job1Id);
                stmt.setLong(2, attrTypeId);
                stmt.setLong(3, job2id);
                stmt.setLong(4, attrType2Id);
                try (ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException(ex.getMessage());
        }
        return ret;
    }

    private final static String SQL_BYJOB = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE job_id=? AND job.job_state=?";

    public AutoCloseableIterator<Attribute> byJob(long jobId) throws MGXException {

//        Job job = getController().getJobDAO().getById(jobId);

        // pre-collect attribute types
        final TLongObjectMap<AttributeType> attrTypes = new TLongObjectHashMap<>();
        try (DBIterator<AttributeType> aTypes = getController().getAttributeTypeDAO().byJob(jobId)) {
            while (aTypes.hasNext()) {
                AttributeType attrType = aTypes.next();
                attrTypes.put(attrType.getId(), attrType);
            }
        }

        List<Attribute> attrs = new ArrayList<>();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BYJOB)) {
                stmt.setLong(1, jobId);
                stmt.setInt(2, JobState.FINISHED.getValue());
                try (ResultSet rs = stmt.executeQuery()) {

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
            throw new MGXException(ex.getMessage());
        }

        attrTypes.clear();

        return new ForwardingIterator<>(attrs.iterator());
    }

    public AutoCloseableIterator<Attribute> bySeqRun(long runId) throws MGXException {

        List<Attribute> attrs = new ArrayList<>();

        try (AutoCloseableIterator<Job> jobIter = getController().getJobDAO().bySeqRun(runId)) {
            while (jobIter.hasNext()) {
                Job job = jobIter.next();
                try (AutoCloseableIterator<Attribute> attrIter = getController().getAttributeDAO().byJob(job.getId())) {
                    while (attrIter.hasNext()) {
                        attrs.add(attrIter.next());
                    }
                }
            }
        }

        return new ForwardingIterator<>(attrs.iterator());
    }

    private final static String SQL_FIND = "SELECT attr.id, value, attrtype_id, job_id, parent_id FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE job.seqrun_id = ANY(?) AND job.job_state=? "
            + "AND upper(attr.value) LIKE CONCAT('%', upper(?), '%')";

    public DBIterator<String> find(String term, List<Long> seqrunIdList) throws MGXException {
        if (term.isEmpty() || seqrunIdList.isEmpty()) {
            throw new MGXException("Empty search term or empty run list.");
        }
        DBIterator<String> iter = null;

        try {
            Connection conn = getController().getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_FIND);
            stmt.setArray(1, conn.createArrayOf("numeric", seqrunIdList.toArray(new Long[seqrunIdList.size()])));
            stmt.setInt(2, JobState.FINISHED.getValue());
            stmt.setString(3, term);
            ResultSet rset = stmt.executeQuery();
            final ResultSet rs = rset;

            iter = new DBIterator<String>(rs, stmt, conn) {
                @Override
                public String convert(ResultSet rs) throws SQLException {
                    return rs.getString(2);
                }
            };
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

    public DBIterator<Sequence> search(String term, boolean exact, List<Long> seqrunIdList) throws MGXException {
        DBIterator<Sequence> iter = null;

        try {
            Connection conn = getController().getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM searchTerm(?,?,?)");
            stmt.setString(1, term);
            stmt.setBoolean(2, exact);
            stmt.setArray(3, conn.createArrayOf("numeric", seqrunIdList.toArray(new Long[seqrunIdList.size()])));
            ResultSet rset = stmt.executeQuery();

            iter = new DBIterator<Sequence>(rset, stmt, conn) {
                @Override
                public Sequence convert(ResultSet rs) throws SQLException {
                    Sequence s = new Sequence();
                    s.setId(rs.getLong(1));
                    s.setName(rs.getString(2));
                    s.setLength(rs.getInt(3));
                    return s;
                }
            };
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

}
