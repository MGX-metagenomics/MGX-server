package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.JobState;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class AttributeTypeDAO<T extends AttributeType> extends DAO<T> {

    @Override
    Class getType() {
        return AttributeType.class;
    }
    
    public List<AttributeType> listTypes() {

        List<AttributeType> types = new ArrayList<AttributeType>();

        final String sql = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type "
                + "FROM attributetype atype "
                + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE job.job_state=? "
                + "GROUP BY atype.id, atype.name, atype.value_type "
                + "ORDER BY atype.name ASC";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setInt(1, JobState.FINISHED.getValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                AttributeType aType = new AttributeType();
                aType.setId(rs.getLong(1));
                aType.setName(rs.getString(2));
                aType.setValueType(rs.getString(3));
                types.add(aType);
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            close(c, stmt, rs);
        }

        return types;
    }

    /*
     *  returns attribute types for a job, if it's in FINISHED state
     */
    public List<AttributeType> listTypesByJob(Long jobId) {

        List<AttributeType> ret = new ArrayList<AttributeType>();

        final String sql = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type "
                + "FROM attributetype atype "
                + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE job.id=? AND job.job_state=? "
                + "GROUP BY atype.id, atype.name, atype.value_type "
                + "ORDER BY atype.name ASC";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setLong(1, jobId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                AttributeType aType = new AttributeType();
                aType.setId(rs.getLong(1));
                aType.setName(rs.getString(2));
                aType.setValueType(rs.getString(3));
                ret.add(aType);
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            close(c, stmt, rs);
        }

        return ret;
    }

    public List<AttributeType> listTypesBySeqRun(Long seqrunId) {

        List<AttributeType> ret = new ArrayList<AttributeType>();

        final String sql = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type "
                + "FROM attributetype atype "
                + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE job.seqrun_id=? AND job.job_state=? "
                + "GROUP BY atype.id, atype.name, atype.value_type "
                + "ORDER BY atype.name ASC";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setLong(1, seqrunId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                AttributeType aType = new AttributeType();
                aType.setId(rs.getLong(1));
                aType.setName(rs.getString(2));
                aType.setValueType(rs.getString(3));
                ret.add(aType);
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            close(c, stmt, rs);
        }

        return ret;
    }
}
