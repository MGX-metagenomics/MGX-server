package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
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

    public DeleteMapping(Long mapId, Connection conn, String projName) {
        super(projName, conn);
        id = mapId;
    }

    @Override
    public void run() {
        Set<Long> jobs = new HashSet<>();
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE id=? RETURNING bam_file, job_id")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    File file = new File(rs.getString(1));
                    if (file.exists()) {
                        file.delete();
                    }
                    jobs.add(rs.getLong(2));
                }
            }

            // delete jobs
            for (Long jobId : jobs) {
                TaskI delJob = new DeleteJob(jobId, conn, getProjectName());
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
