package de.cebitec.mgx.workers;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitter;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sj
 */
public class RestartJob extends TaskI {

    private final Job job;
    private final JobSubmitter js;
    private final String dispatcherHost;
    private final MGXConfigurationI mgxcfg;
    private final String dbHost;
    private final String dbName;
    private final File projDir;

    public RestartJob(MGXController mgx, String dispatcherHost, MGXConfigurationI cfg, Job job, DataSource dataSource, String projName, JobSubmitter js) throws IOException, MGXDispatcherException {
        super(projName, dataSource);
        this.job = job;
        this.dispatcherHost = dispatcherHost;
        this.js = js;
        mgxcfg = cfg;
        dbHost = mgx.getDatabaseHost();
        dbName = mgx.getDatabaseName();
        projDir = mgx.getProjectDirectory();
    }

    @Override
    public void run() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                        + "(SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }
            }

            // delete attributes
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }
            }

            // delete mappings
            setStatus(TaskI.State.PROCESSING, "Deleting mappings");
            try (Connection conn = dataSource.getConnection()) {
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
            }

            setStatus(TaskI.State.PROCESSING, "Validating job configuration");
            boolean verified = false;

            verified = js.validate(projName, dataSource, job, dispatcherHost, dbHost, dbName, mgxcfg.getMGXUser(), mgxcfg.getMGXPassword(), projDir);

            if (verified) {
                setStatus(TaskI.State.PROCESSING, "Resubmitting job..");
                if (js.submit(dispatcherHost, dataSource, projName, job)) {
                    setStatus(TaskI.State.FINISHED, "Job " + job.getId() + " restarted");
                } else {
                    setStatus(TaskI.State.FAILED, "submit failed");
                }
            } else {
                setStatus(TaskI.State.FAILED, "Verification failed");
            }

//            conn.close();
//            conn = null;
        } catch (MGXDispatcherException | SQLException e) {
            System.err.println("Could not restart job " + job.getId() + ": " + e.getMessage());
            Logger.getLogger(RestartJob.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
