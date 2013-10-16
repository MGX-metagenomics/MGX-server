package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public final class DeleteJob extends TaskI {

    private final long id;

    public DeleteJob(long id, Connection conn, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // delete attributes
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // delete mappings
            setStatus(TaskI.State.PROCESSING, "Deleting mapping data for job " + id);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE job_id=? RETURNING bam_file")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        File bamFile = new File(rs.getString(1));
                        if (bamFile.exists()) {
                            bamFile.delete();
                        }
                    }
                }
            }

            // parameters
            setStatus(TaskI.State.PROCESSING, "Deleting job parameters for job " + id);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM jobparameter WHERE job_id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // job
            setStatus(TaskI.State.PROCESSING, "Deleting job " + id);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM job WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            setStatus(TaskI.State.FINISHED, "Job " + id + " deleted");
        } catch (Exception e) {
            Logger.getLogger(DeleteJob.class.getName()).log(Level.SEVERE, "Could not delete job " + id, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}