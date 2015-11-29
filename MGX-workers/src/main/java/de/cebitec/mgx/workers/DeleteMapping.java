package de.cebitec.mgx.workers;

import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.core.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class DeleteMapping extends TaskI {

    private final long id;
    private final MappingSessions sessions;

    public DeleteMapping(Long mapId, Connection conn, String projName, MappingSessions sessions) {
        super(projName, conn);
        this.sessions = sessions;
        id = mapId;
    }

    @Override
    public void run() {
        
        // abort any sessions referring to this mapping
        sessions.abort(id);
        
        Set<Long> jobs = new HashSet<>();
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE id=? RETURNING bam_file, job_id")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String bamName = rs.getString(1);
                    File bamFile = new File(bamName);
                    if (bamFile.exists()) {
                        bamFile.delete();
                    }
                    File bamIdx = new File(bamName + ".bai");
                    if (bamIdx.exists()) {
                        bamIdx.delete();
                    }
                    File covFile = new File(bamName + ".maxCov");
                    if (covFile.exists()) {
                        covFile.delete();
                    }
                    jobs.add(rs.getLong(2));
                }
            }

            // delete jobs
            for (Long jobId : jobs) {
                TaskI delJob = new DeleteJob(jobId, conn, getProjectName(), sessions);
                delJob.addPropertyChangeListener(this);
                delJob.run();
                delJob.removePropertyChangeListener(this);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DeleteMapping.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(TaskI.State.FAILED, ex.getMessage());
        }
    }
}
