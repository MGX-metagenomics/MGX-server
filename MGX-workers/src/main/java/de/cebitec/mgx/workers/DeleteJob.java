package de.cebitec.mgx.workers;

import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.core.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public final class DeleteJob extends TaskI {

    private final long id;
    private final MappingSessions mapSessions;

    public DeleteJob(long id, DataSource dataSource, String projName, MappingSessions mapSessions) {
        super(projName, dataSource);
        this.id = id;
        this.mapSessions = mapSessions;
    }

    @Override
    public void run() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                        + "(SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            // delete attributes
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            // delete mappings
            setStatus(TaskI.State.PROCESSING, "Deleting mapping data for job " + id);
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE job_id=? RETURNING id, bam_file")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {

                            // abort any sessions that might use this mapping
                            long mappingId = rs.getLong(1);
                            mapSessions.abort(mappingId);

                            // remove persistent data
                            String bamName = rs.getString(2);
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
                        }
                    }
                }
            }

            // parameters
            setStatus(TaskI.State.PROCESSING, "Deleting job parameters for job " + id);
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM jobparameter WHERE job_id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            // job
            setStatus(TaskI.State.PROCESSING, "Deleting job " + id);
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM job WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            setStatus(TaskI.State.FINISHED, "Job " + id + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteJob.class.getName()).log(Level.SEVERE, "Could not delete job " + id, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
