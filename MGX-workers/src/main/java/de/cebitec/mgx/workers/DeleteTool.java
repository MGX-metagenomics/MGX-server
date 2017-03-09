package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.core.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public final class DeleteTool extends TaskI {

    private final long id;
    private final MappingSessions mappingSessions;

    public DeleteTool(GPMSManagedDataSourceI dataSource, long id, String projName, MappingSessions mappingSessions) {
        super(projName, dataSource);
        this.id = id;
        this.mappingSessions = mappingSessions;
    }

    @Override
    public void run() {
        // fetch jobs for this tool
        List<Long> jobs = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM job WHERE tool_id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        jobs.add(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete jobs
        for (Long jobId : jobs) {
            TaskI delJob = new DeleteJob(jobId, getDataSource(), getProjectName(), mappingSessions);
            delJob.addPropertyChangeListener(this);
            delJob.run();
            delJob.removePropertyChangeListener(this);
        }

        try {
            String toolName = null;
            String conveyorGraph = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name, xml_file FROM tool WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            toolName = rs.getString(1);
                            conveyorGraph = rs.getString(2);
                        }
                    }
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting tool " + toolName);
            if (conveyorGraph != null) {
                File f = new File(conveyorGraph);
                if (f.exists()) {
                    f.delete();
                }
            }
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM tool WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }
            setStatus(TaskI.State.FINISHED, toolName + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteTool.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
