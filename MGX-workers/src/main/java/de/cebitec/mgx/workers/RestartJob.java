package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dispatcher.common.api.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.Host;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class RestartJob extends TaskI {

    private final Job job;
    private final JobSubmitterI js;
    private final String dispatcherHost;

    public RestartJob(String dispatcherHost, Job job, GPMSManagedDataSourceI dataSource, String projName, JobSubmitterI js) throws IOException, MGXDispatcherException {
        super(projName, dataSource);
        this.job = job;
        this.dispatcherHost = dispatcherHost;
        this.js = js;
    }

    @Override
    public void process() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                }
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                        + "(SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }
            }

            // delete attributes
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }
            }

            // delete mappings
            setStatus(TaskI.State.PROCESSING, "Deleting mappings");
            try (Connection conn = getConnection()) {
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

            verified = js.validate(new Host(dispatcherHost), projName, job.getId());

            if (verified) {
                job.setStatus(JobState.VERIFIED);
                setStatus(TaskI.State.PROCESSING, "Resubmitting job..");
                if (js.submit(new Host(dispatcherHost), projName, job.getId())) {
                    setStatus(TaskI.State.FINISHED, "Job " + job.getId() + " restarted");
                } else {
                    setStatus(TaskI.State.FAILED, "submit failed");
                }
            } else {
                job.setStatus(JobState.FAILED);
                setStatus(TaskI.State.FAILED, "Verification failed");
            }

//            conn.close();
//            conn = null;
        } catch (MGXDispatcherException | SQLException e) {
//            System.err.println("Could not restart job " + job.getId() + ": " + e.getMessage());
            Logger.getLogger(RestartJob.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
