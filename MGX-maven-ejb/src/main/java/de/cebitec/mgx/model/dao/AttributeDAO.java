package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.util.Triple;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class AttributeDAO<T extends Attribute> extends DAO<T> {

    @Override
    Class getType() {
        return Attribute.class;
    }

    public Set<String> listTypes() {
        
        Set<String> types = new HashSet<String>();
        
        final String sql = "SELECT DISTINCT type FROM attribute WHERE id IN (SELECT DISTINCT attributeid "
                + "FROM job RIGHT JOIN observation ON (observation.jobid = job.id) WHERE job.job_state=?)";
        
        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setInt(1, JobState.FINISHED.getValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                types.add(rs.getString(1));
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
    public Set<String> listTypesByJob(Long jobId) {

        Set<String> ret = new HashSet<String>();

        final String sql = "SELECT DISTINCT type FROM attribute a "
                + "RIGHT JOIN observation o ON (o.attributeid = a.id) "
                + "RIGHT JOIN job j ON (o.jobid = j.id) where o.jobid=? AND j.job_state=?";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setLong(1, jobId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            res = stmt.executeQuery();
            while (res.next()) {
                ret.add(res.getString(1));
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            close(c, stmt, res);
        }

        return ret;
    }

    public Map<Triple<Long, String, Long>, Long> getDistribution(String attrName, Long job_id, List<Long> seqrun_ids) throws MGXException {

        if (attrName == null) {
            throw new MGXException("Missing attribute name");
        }

        StringBuilder sql = new StringBuilder("SELECT attribute.id, attribute.value, attribute.parent_id, count(attribute.value) FROM observation ")
                .append("LEFT JOIN attribute ON (observation.attributeid = attribute.id) ")
                .append("LEFT JOIN read ON (observation.seqid = read.id) ")
                .append("WHERE attribute.type=?");

        // process the seqrun_id's ..
        if (!seqrun_ids.isEmpty()) {
            sql.append(" AND read.seqrun_id in (");
            sql.append(toSQLTemplateString(seqrun_ids));
            sql.append(")");
        }

        if (job_id != null) {
            sql.append(" AND observation.jobid=?");
        }

        sql.append(" GROUP BY attribute.id, type, value, parent_id ORDER BY value");

        int param_pos = 1;
        Map<Triple<Long, String, Long>, Long> ret = new HashMap<Triple<Long, String, Long>, Long>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet res = null;
        
        try {
            stmt = conn.prepareStatement(sql.toString());
            stmt.setString(param_pos++, attrName);

            for (Long id : seqrun_ids) {
                stmt.setLong(param_pos++, id);
            }

            if (job_id != null) {
                stmt.setLong(param_pos++, job_id);
            }

            res = stmt.executeQuery();
            while (res.next()) {
                // the triple represents the attribute: id, label, parent_id
                Triple<Long, String, Long> attr = new Triple<Long, String, Long>(res.getLong(1), res.getString(2), res.getLong(3));
                Long count = res.getLong(4);
                ret.put(attr, count);
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, res);
        }

        return ret;
    }
}
