package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class JobParameterDAO extends DAO<JobParameter> {

    public JobParameterDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return JobParameter.class;
    }

    private final static String CREATE = "INSERT INTO jobparameter (job_id, node_id, param_name, param_value, user_name, user_desc) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(JobParameter obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setLong(1, obj.getJobId());
                stmt.setLong(2, obj.getNodeId());
                stmt.setString(3, obj.getParameterName());
                stmt.setString(4, obj.getParameterValue());
                stmt.setString(5, obj.getUserName());
                stmt.setString(6, obj.getUserDescription());

                try (ResultSet rs = stmt.executeQuery()) {
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

    private final static String UPDATE = "UPDATE jobparameter SET job_id=?, node_id=?, param_name=?, param_value=?, user_name=?, user_desc=?"
            + " WHERE id=?";

    public void update(JobParameter obj) throws MGXException {
        if (obj.getId() == JobParameter.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setLong(1, obj.getJobId());
                stmt.setLong(2, obj.getNodeId());
                stmt.setString(3, obj.getParameterName());
                stmt.setString(4, obj.getParameterValue());
                stmt.setString(5, obj.getUserName());
                stmt.setString(6, obj.getUserDescription());

                stmt.setLong(7, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public void delete(long id) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM jobparameter WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }
    
    private final static String BY_ID = "SELECT id, job_id, node_id, param_name, param_value, user_name, user_desc FROM jobparameter WHERE id=?";

    @Override
    public JobParameter getById(final long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }
                    
                    JobParameter jp = new JobParameter();
                    jp.setId(rs.getLong(1));
                    jp.setJobId(rs.getLong(2));
                    jp.setNodeId(rs.getLong(3));
                    jp.setParameterName(rs.getString(4));
                    jp.setParameterValue(rs.getString(5));
                    jp.setUserName(rs.getString(6));
                    jp.setUserDescription(rs.getString(7));
                            
                    return jp;
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public AutoCloseableIterator<JobParameter> byJob(final long job_id) throws MGXException {
        final Job job = getController().getJobDAO().getById(job_id);
        return new ForwardingIterator<>(job.getParameters().iterator());
    }
}
