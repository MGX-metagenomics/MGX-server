package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public final class DeleteJob extends TaskI {

    private static final String[] suffices = {"", ".stdout", ".stderr"};
    private final long id;
    private final String jobdir;

    public DeleteJob(long id, GPMSManagedDataSourceI dataSource, String projName, String jobDir) {
        super(projName, dataSource);
        this.id = id;
        this.jobdir = jobDir;
    }

    @Override
    public void process() {
        try {

            String sb;
            sb = new StringBuilder(jobdir)
                    .append(File.separator)
                    .append(id).toString();

            for (String suffix : suffices) {
                File f = new File(sb + suffix);
                if (f.exists()) {
                    f.delete();
                }
            }

            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM gene_observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                        + "(SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            // delete attributes
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            // delete mappings
            setStatus(TaskI.State.PROCESSING, "Deleting mapping data for job " + id);
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE job_id=? RETURNING bam_file")) {
                    stmt.setLong(1, id);
                    try ( ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            // remove persistent data
                            String bamName = rs.getString(1);
                            File bamFile = new File(bamName);
                            if (bamFile.exists()) {
                                bamFile.delete();
                            }
                            File bamIdx = new File(bamName + ".bai");
                            if (bamIdx.exists()) {
                                bamIdx.delete();
                            }
                            File maxCov = new File(bamName + ".maxCov");
                            if (maxCov.exists()) {
                                maxCov.delete();
                            }
                            File refCov = new File(bamName + ".refCov");
                            if (refCov.exists()) {
                                refCov.delete();
                            }
                        }
                    }
                }
            }

            // parameters
            setStatus(TaskI.State.PROCESSING, "Deleting job parameters for job " + id);
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM jobparameter WHERE job_id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            // job
            setStatus(TaskI.State.PROCESSING, "Deleting job " + id);
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM job WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            setStatus(TaskI.State.FINISHED, "Job " + id + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteJob.class.getName()).log(Level.SEVERE, "Could not delete job " + id, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
