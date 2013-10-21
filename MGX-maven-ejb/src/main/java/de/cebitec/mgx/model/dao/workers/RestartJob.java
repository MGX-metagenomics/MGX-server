package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.jobsubmitter.JobSubmitter;
import de.cebitec.mgx.sessions.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class RestartJob extends TaskI {

    private final long id;
    private final JobSubmitter js;

    public RestartJob(long id, Connection conn, String projName, JobSubmitter js) {
        super(projName, conn);
        this.id = id;
        this.js = js;
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
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE job_id=? RETURNING bam_file")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        File bam = new File(rs.getString(1));
                        if (bam.exists()) {
                            bam.delete();
                        }
                    }
                }
            }

            boolean verified = false;
            //verified = js.validate(mgx, id);


            setStatus(TaskI.State.FINISHED, "Job " + id + " restarted");
        } catch (Exception e) {
            Logger.getLogger(DeleteJob.class.getName()).log(Level.SEVERE, "Could not restart job " + id, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
