package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sjaenick
 */
public class AttributeDAO<T extends Attribute> extends DAO<T> {

    private final static String FETCH_DIST = "SELECT attr.id as attr_id, attr.value, attrcount.cnt "      
                + "FROM attribute attr "
                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "LEFT JOIN attributecount attrcount ON (attr.id = attrcount.attr_id) "
                + "WHERE attr.attrtype_id=? "
                + "AND attr.job_id=?";
    
    private final static String FETCH_HIERARCHY = "WITH RECURSIVE subattributes AS "
                + "( "
                + "WITH attributecounts AS ( "
                + "SELECT attr.attrtype_id as attrtype_id, atype.name as attrtype_name, atype.structure as atype_structure, atype.value_type as attrtype_valtype, attr.id as attr_id, attr.value as attr_value, attr.parent_id as parent_id, attrcount.cnt as count "
                + "FROM attribute attr "
                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "LEFT JOIN attributecount attrcount ON (attr.id = attrcount.attr_id) "
                + "WHERE attr.job_id=? "
                + ") "
                + "SELECT * FROM attributecounts WHERE attr_id=( "
                + "WITH RECURSIVE findroot AS ( "
                + "WITH hierarchy AS ( "
                + "SELECT attr.attrtype_id as attrtype_id, attr.id as attr_id, attr.parent_id as parent_id "
                + "FROM attribute attr "
                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "WHERE attr.job_id=? "
                + ") "
                + "SELECT * FROM hierarchy WHERE attrtype_id=? "
                + "UNION "
                + "SELECT parent.* FROM hierarchy AS parent "
                + "JOIN "
                + "findroot AS fr "
                + "ON (fr.parent_id = parent.attr_id) "
                + ") "
                + "SELECT attr_id from findroot WHERE parent_id IS NULL "
                + ") "
                + "UNION "
                + "SELECT temp.* "
                + "FROM "
                + "attributecounts AS temp "
                + "JOIN "
                + "subattributes AS sa "
                + "ON (sa.attr_id = temp.parent_id) "
                + ") "
                + "SELECT attrtype_id, attrtype_name, atype_structure, attrtype_valtype, attr_id, attr_value, parent_id, count FROM subattributes";
            
    @Override
    Class getType() {
        return Attribute.class;
    }

    public Map<Attribute, Long> getDistribution(Long attrTypeId, Job job) throws MGXException {
        
        AttributeType attrType = getController().getAttributeTypeDAO().getById(attrTypeId);

//        final String sql = "SELECT attr.id as attr_id, attr.value, count(attr.value) "
//                + "FROM observation obs "
//                + "LEFT JOIN attribute attr ON (obs.attributeid = attr.id) "
//                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
//                + "LEFT JOIN read ON (obs.seqid = read.id) "
//                + "LEFT JOIN job ON (attr.job_id = job.id) "
//                + "WHERE attr.attrtype_id=? "
//                + "AND attr.job_id=? "
//                + "AND job.seqrun_id=read.seqrun_id "
//                + "AND job.job_state=? "
//                + "GROUP BY attr.id, attr.attrtype_id, attr.value ORDER BY attr.value";


        Map<Attribute, Long> ret = new HashMap<>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement(FETCH_DIST);
            stmt.setLong(1, attrTypeId);
            stmt.setLong(2, job.getId());
            //stmt.setInt(3, JobState.FINISHED.getValue());
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

    public Map<Attribute, Long> getHierarchy(Long attrTypeId, Job job) throws MGXException {

//        final String sql = "WITH RECURSIVE subattributes AS "
//                + "( "
//                + "WITH attributecount AS ( "
//                + "SELECT attr.attrtype_id as attrtype_id, atype.name as attrtype_name, atype.structure as atype_structure, atype.value_type as attrtype_valtype, attr.id as attr_id, attr.value as attr_value, attr.parent_id as parent_id, count(attr.value) as count FROM observation obs "
//                + "LEFT JOIN attribute attr ON (obs.attributeid = attr.id) "
//                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
//                + "WHERE attr.job_id=? "
//                + "GROUP BY attrtype_id, attrtype_name, atype_structure, attrtype_valtype, attr_id, attr_value, parent_id "
//                + "ORDER BY attr_value "
//                + ") "
//                + "SELECT * FROM attributecount WHERE attr_id=( "
//                + "WITH RECURSIVE findroot AS ( "
//                + "WITH hierarchy AS ( "
//                + "SELECT attr.attrtype_id as attrtype_id, attr.id as attr_id, attr.parent_id as parent_id "
//                + "FROM attribute attr "
//                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
//                + "WHERE attr.job_id=? "
//                + ") "
//                + "SELECT * FROM hierarchy WHERE attrtype_id=? "
//                + "UNION "
//                + "SELECT parent.* FROM hierarchy AS parent "
//                + "JOIN "
//                + "findroot AS fr "
//                + "ON (fr.parent_id = parent.attr_id) "
//                + ") "
//                + "SELECT attr_id from findroot WHERE parent_id IS NULL "
//                + ") "
//                + "UNION "
//                + "SELECT temp.* "
//                + "FROM "
//                + "attributecount AS temp "
//                + "JOIN "
//                + "subattributes AS sa "
//                + "ON (sa.attr_id = temp.parent_id) "
//                + ") "
//                + "SELECT attrtype_id, attrtype_name, atype_structure, attrtype_valtype, attr_id, attr_value, parent_id, count FROM subattributes "
//                + "ORDER BY attr_value";

        Map<Attribute, Long> ret = new HashMap<>();
        Map<Long, AttributeType> aTypeCache = new HashMap<>();
        Map<Long, Attribute> attrCache = new HashMap<>();
        Map<Long, Long> attr2parent = new HashMap<>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.prepareStatement(FETCH_HIERARCHY);
            stmt.setLong(1, job.getId());
            stmt.setLong(2, job.getId());
            stmt.setLong(3, attrTypeId);
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
        
        //System.err.println("DAO returning hierarchy with "+ret.keySet().size()+" entries");

        return ret;
    }
}
