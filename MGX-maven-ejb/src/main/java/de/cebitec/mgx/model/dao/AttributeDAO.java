package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.Observation;
import de.cebitec.mgx.util.Pair;
import de.cebitec.mgx.util.SearchResult;
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

    @Override
    Class getType() {
        return Attribute.class;
    }

    public Map<Attribute, Long> getDistribution(long attrTypeId, Job job) throws MGXException {

        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);

        Map<Attribute, Long> ret = new HashMap<>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement("SELECT * FROM getDistribution(?,?)");
            stmt.setLong(1, attrTypeId);
            stmt.setLong(2, job.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Attribute attr = new Attribute();
                attr.setAttributeType(attrType);
                attr.setJob(job);
                //
                attr.setId(rs.getLong(1));
                attr.setValue(rs.getString(2));
                ret.put(attr, rs.getLong(3));
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, rs);
        }

        return ret;
    }

    public Map<Attribute, Long> getHierarchy(long attrTypeId, Job job) throws MGXException {

        Map<Attribute, Long> ret = new HashMap<>();
        Map<Long, AttributeType> aTypeCache = new HashMap<>();
        Map<Long, Attribute> attrCache = new HashMap<>();
        Map<Long, Long> attr2parent = new HashMap<>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement("SELECT * FROM getHierarchy(?,?)");
            stmt.setLong(1, attrTypeId);
            stmt.setLong(2, job.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                //  attrtype_id | attrtype_name | atype_structure | attrtype_valtype | attr_id | attr_value | parent_id | count
                AttributeType aType;
                Long aTypeID = rs.getLong(1);
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

                Long parentId = rs.getLong(7);
                attr2parent.put(attr.getId(), parentId);
                attrCache.put(attr.getId(), attr);

                ret.put(attr, rs.getLong(8));
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, rs);
        }

        for (Attribute a : ret.keySet()) {
            Long parentID = attr2parent.get(a.getId());
            if (parentID != null) {
                Attribute parent = attrCache.get(parentID);
                a.setParent(parent);
            }
        }

        return ret;
    }

    public Map<Pair<Attribute, Attribute>, Long> getCorrelation(long attrTypeId, Job job, long attrType2Id, Job job2) throws MGXException {

        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);
        AttributeType attrType2 = getController().getAttributeTypeDAO().getById(attrType2Id);

        // test code - bulk retrieval
        for (AttributeType at : getController().getAttributeTypeDAO().getByIds(Arrays.asList(attrTypeId, attrType2Id))) {
            System.err.println("got AT: " + at.getName());
        }

        Map<Pair<Attribute, Attribute>, Long> ret = new HashMap<>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement("SELECT * FROM getCorrelation(?,?,?,?)");
            stmt.setLong(1, job.getId());
            stmt.setLong(2, attrTypeId);
            stmt.setLong(3, job2.getId());
            stmt.setLong(4, attrType2Id);
            rs = stmt.executeQuery();
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

                long cnt = rs.getLong(5);

                ret.put(new Pair<>(attr, attr2), cnt);
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, rs);
        }
        return ret;
    }

    public Collection<SearchResult> search(String term, boolean exact, List<Long> seqrunIdList) throws MGXException {
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, SearchResult> resultBySeqName = new HashMap<>();

        try {
            stmt = conn.prepareStatement("SELECT * FROM searchTerm(?,?)");
            stmt.setString(1, term);
            stmt.setArray(2, conn.createArrayOf("BIGINT", seqrunIdList.toArray(new Long[seqrunIdList.size()])));
            rs = stmt.executeQuery();
            while (rs.next()) {
                long seqId = rs.getLong(1);
                String seqName = rs.getString(2);
                int seqLength = rs.getInt(3);
                
                SearchResult sr;
                if (resultBySeqName.containsKey(seqName)) {
                    sr = resultBySeqName.get(seqName);
                } else {
                    sr = new SearchResult();
                    sr.setSequenceName(seqName);
                    sr.setSequenceLength(seqLength);
                    resultBySeqName.put(seqName, new SearchResult());
                }
                
                Observation obs = new Observation();
                obs.setStart(rs.getInt(4));
                obs.setStop(rs.getInt(5));
                

                sr.addObservation(obs);
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, rs);
        }
        
        return resultBySeqName.values();
    }
}
