package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
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
public class AttributeDAO<T extends Attribute> extends DAO<T> {

    public AttributeDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class<Attribute> getType() {
        return Attribute.class;
    }

    public List<Triple<Attribute, Long, Long>> getDistribution(long attrTypeId, long jobId) throws MGXException {

        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);
        Job job = getController().getJobDAO().getById(jobId);

        // attribute, parent id, count
        List<Triple<Attribute, Long, Long>> ret = new LinkedList<>();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getDistribution(?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, jobId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setAttributeType(attrType);
                        attr.setJob(job);
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

    public Map<Attribute, Long> getHierarchy(long attrTypeId, Job job) throws MGXException {

        Map<Attribute, Long> ret = new HashMap<>();
        TLongObjectMap<AttributeType> aTypeCache = new TLongObjectHashMap<>();
        TLongObjectMap<Attribute> attrCache = new TLongObjectHashMap<>();
        TLongLongMap attr2parent = new TLongLongHashMap();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getHierarchy(?,?)")) {
                stmt.setLong(1, attrTypeId);
                stmt.setLong(2, job.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        //  attrtype_id | attrtype_name | atype_structure | attrtype_valtype | attr_id | attr_value | parent_id | count
                        AttributeType aType;
                        long aTypeID = rs.getLong(1);
                        if (aTypeCache.containsKey(aTypeID)) {
                            aType = aTypeCache.get(aTypeID);
                        } else {
                            aType = new AttributeType();
                            aType.setId(aTypeID);
                            aType.setName(rs.getString(2));
                            aType.setStructure(rs.getString(3).charAt(0));
                            aType.setValueType(rs.getString(4).charAt(0));
                            aTypeCache.put(aTypeID, aType);
                        }

                        Attribute attr = new Attribute();
                        attr.setAttributeType(aType);
                        attr.setJob(job);
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
                a.setParent(parent);
            }
        }

        return ret;
    }

    public Map<Pair<Attribute, Attribute>, Integer> getCorrelation(long attrTypeId, Job job, long attrType2Id, Job job2) throws MGXException {

        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);
        AttributeType attrType2 = getController().getAttributeTypeDAO().getById(attrType2Id);

        // test code - bulk retrieval
//        for (AttributeType at : getController().getAttributeTypeDAO().getByIds(Arrays.asList(attrTypeId, attrType2Id))) {
//            System.err.println("got AT: " + at.getName());
//        }
        Map<Pair<Attribute, Attribute>, Integer> ret = new HashMap<>();

        try (Connection conn = getController().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM getCorrelation(?,?,?,?)")) {
                stmt.setLong(1, job.getId());
                stmt.setLong(2, attrTypeId);
                stmt.setLong(3, job2.getId());
                stmt.setLong(4, attrType2Id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Attribute attr = new Attribute();
                        attr.setAttributeType(attrType);
                        attr.setJob(job);
                        //
                        attr.setId(rs.getLong(1));
                        attr.setValue(rs.getString(2));

                        Attribute attr2 = new Attribute();
                        attr2.setAttributeType(attrType2);
                        attr2.setJob(job2);
                        //
                        attr2.setId(rs.getLong(3));
                        attr2.setValue(rs.getString(4));

                        int cnt = rs.getInt(5);

                        ret.put(new Pair<>(attr, attr2), cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return ret;
    }

    private final static String SQL_BYJOB = "SELECT attr.id, attr.value, attr.attrtype_id, attr.parent_id FROM attribute attr "
            + "LEFT JOIN job ON (attr.job_id = job.id) "
            + "WHERE job_id=? AND job.job_state=?";

    public AutoCloseableIterator<Attribute> ByJob(long jobId) throws MGXException {

        // pre-collect attribute types
        final TLongObjectMap<AttributeType> attrTypes = new TLongObjectHashMap<>();
        try (DBIterator<AttributeType> aTypes = getController().getAttributeTypeDAO().ByJob(jobId)) {
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
                        attr.setValue(rs.getString(2));

                        AttributeType aType = attrTypes.get(rs.getLong(3));
                        attr.setAttributeType(aType);

                        // fetch parent, if present
                        long parentId = rs.getLong(4);
                        if (parentId != 0) {
                            Attribute parent = getController().getAttributeDAO().getById(parentId);
                            attr.setParent(parent);
                        }

                        attrs.add(attr);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }

        attrTypes.clear();

        return new ForwardingIterator<>(attrs.iterator());
    }

    public AutoCloseableIterator<Attribute> BySeqRun(Long runId) throws MGXException {

        SeqRun run = getController().getSeqRunDAO().getById(runId);

        List<Attribute> attrs = new ArrayList<>();

        try (AutoCloseableIterator<Job> jobIter = getController().getJobDAO().BySeqRun(run)) {
            while (jobIter.hasNext()) {
                Job job = jobIter.next();
                try (AutoCloseableIterator<Attribute> attrIter = getController().getAttributeDAO().ByJob(job.getId())) {
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
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }

    public DBIterator<Sequence> search(String term, boolean exact, List<Long> seqrunIdList) throws MGXException {
        DBIterator<Sequence> iter = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;

        try {
            Connection conn = getController().getConnection();
            stmt = conn.prepareStatement("SELECT * FROM searchTerm(?,?,?)");
            stmt.setString(1, term);
            stmt.setBoolean(2, exact);
            stmt.setArray(3, conn.createArrayOf("numeric", seqrunIdList.toArray(new Long[seqrunIdList.size()])));
            rset = stmt.executeQuery();
            final ResultSet rs = rset;

            iter = new DBIterator<Sequence>(rs, stmt, conn) {
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
