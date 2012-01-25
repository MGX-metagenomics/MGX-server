package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
import java.io.File;
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
public class JobDAO<T extends Job> extends DAO<T> {

    private static String[] suffices = {"", ".xml", ".stdout", ".stderr"};

    @Override
    Class getType() {
        return Job.class;
    }

    @Override
    public void delete(Long id) {

        /*
         * here, we only delete the persistent files that might have
         * been created by executing a job. The dispatcher will handle
         * deletion of generated observations and orphan attributes
         */

        StringBuilder sb = new StringBuilder(getController().getProjectDirectory()).append(File.separator).append(id);

        boolean all_deleted = true;
        for (String suffix : suffices) {
            File f = new File(sb.toString() + suffix);
            if (f.exists()) {
                boolean deleted = f.delete();
                all_deleted = all_deleted && deleted;
            }
        }
    }

    public Iterable<Job> BySeqRun(SeqRun sr) {
        return getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun").
                setParameter("seqrun", sr).getResultList();
    }
    
//    public Iterable<Job> ByAttributeTypeAndSeqRun(Long atype_id, Long seqrun_id) throws MGXException {
//        
//        // FIXME - this us ugly, use JPQL
//        final String sql = "SELECT DISTINCT job.id FROM job "
//                + "LEFT JOIN attribute attr ON (job.id = attr.job_id) "
//                + "WHERE job.job_state=? AND job.seqrun_id=1 AND attr.attrtype_id=?";
//        
//        List<Job> ret = new ArrayList<Job>();
//        Connection conn = getController().getConnection();
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        try {
//            stmt = conn.prepareStatement(sql);
//            stmt.setInt(1, JobState.FINISHED.getValue());
//            stmt.setLong(2, seqrun_id);
//            stmt.setLong(3, atype_id);
//            rs = stmt.executeQuery();
//            while (rs.next()) {
//                Long jobId = rs.getLong(1);
//                ret.add(getById(jobId));
//            }
//        } catch (SQLException ex) {
//            throw new MGXException(ex.getMessage());
//        } finally {
//            close(conn, stmt, rs);
//        }
//
//        return ret;
//    }
}
