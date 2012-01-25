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

    @Override
    Class getType() {
        return Attribute.class;
    }

    public Map<Attribute, Long> getDistribution(AttributeType aType, Job job) throws MGXException {

        final String sql = "SELECT attr.id as attr_id, attr.value, count(attr.value) "
                + "FROM observation obs "
                + "LEFT JOIN attribute attr ON (obs.attributeid = attr.id) "
                + "LEFT JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "LEFT JOIN read ON (obs.seqid = read.id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE attr.attrtype_id=? "
                + "AND attr.job_id=? "
                + "AND job.seqrun_id=read.seqrun_id "
                + "AND job.job_state=? "
                + "GROUP BY attr.id, attr.attrtype_id, attr.value ORDER BY attr.value";


        Map<Attribute, Long> ret = new HashMap<Attribute, Long>();
        Connection conn = getController().getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, aType.getId());
            stmt.setLong(2, job.getId());
            stmt.setInt(3, JobState.FINISHED.getValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Attribute attr = new Attribute();
                attr.setAttributeType(aType);
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
}
