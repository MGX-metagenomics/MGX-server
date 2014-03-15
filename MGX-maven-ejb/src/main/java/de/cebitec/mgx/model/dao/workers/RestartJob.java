package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.JobSubmitter;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.sessions.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sj
 */
public class RestartJob extends TaskI {

    private final Job job;
    private final JobSubmitter js;
    private final String dispatcherHost;
    private final MGXConfiguration mgxcfg;
    private final String dbHost;
    private final String dbName;
    private final String projDir;

    public RestartJob(MGXController mgx, Job job, Connection conn, String projName, String dispatcherHost, JobSubmitter js) {
        super(projName, conn);
        this.job = job;
        this.dispatcherHost = dispatcherHost;
        this.js = js;
        mgxcfg = mgx.getConfiguration();
        dbHost = mgx.getDatabaseHost();
        dbName = mgx.getDatabaseName();
        projDir = mgx.getProjectDirectory();
    }

    @Override
    public void run() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, job.getId());
                stmt.execute();
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, job.getId());
                stmt.execute();
            }

            // delete attributes
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                stmt.setLong(1, job.getId());
                stmt.execute();
            }

            // delete mappings
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE job_id=? RETURNING bam_file")) {
                stmt.setLong(1, job.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        File bam = new File(name);
                        if (bam.exists()) {
                            bam.delete();
                        }
                        File bai = new File(name);
                        if (bai.exists()) {
                            bai.delete();
                        }
                    }
                }
            }

            boolean verified = false;
            verified = js.validate(projName, conn, job, mgxcfg, dbHost, dbName, projDir);

            if (verified) {
                if (js.submit(dispatcherHost, conn, projName, job)) {
                    setStatus(TaskI.State.FINISHED, "Job " + job.getId() + " restarted");
                } else {
                    setStatus(TaskI.State.FAILED, "submit failed");
                }
            } else {
                setStatus(TaskI.State.FAILED, "verify failed");
            }
            
            conn.close();
            conn = null;

        } catch (MGXException | MGXDispatcherException | SQLException e) {
            System.err.println("Could not restart job " + job.getId() + ": " + e.getMessage());
            setStatus(TaskI.State.FAILED, e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
            }
        }
    }
}
