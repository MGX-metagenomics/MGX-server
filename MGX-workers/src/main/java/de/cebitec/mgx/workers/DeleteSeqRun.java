package de.cebitec.mgx.workers;

import de.cebitec.mgx.sequence.SeqReaderFactory;
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
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public final class DeleteSeqRun extends TaskI {

    private final long id;
    private final File projectDir;
    private final MappingSessions mappingSessions;

    public DeleteSeqRun(long id, DataSource dataSource, String projName, File projectDir, MappingSessions mappingSessions) {
        super(projName, dataSource);
        this.id = id;
        this.projectDir = projectDir;
        this.mappingSessions = mappingSessions;
    }

    @Override
    public void run() {

        // fetch jobs for this seqrun
        List<Long> jobs = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM job WHERE seqrun_id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        jobs.add(rs.getLong(1));
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteSeqRun.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete jobs
        for (Long jobId : jobs) {
            TaskI delJob = new DeleteJob(jobId, dataSource, getProjectName(), mappingSessions);
            delJob.addPropertyChangeListener(this);
            delJob.run();
            delJob.removePropertyChangeListener(this);
        }

        try {
            String runName = null;
            String dBFile = null;
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name, dbfile FROM seqrun WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            runName = rs.getString(1);
                            dBFile = rs.getString(2);
                        }
                    }
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting sequencing run " + runName);

            // remove persistent storage file
            if (dBFile != null) {
                SeqReaderFactory.delete(dBFile);
            }

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            File qcDir = new File(projectDir.getAbsolutePath() + File.separator + "QC");
            File[] listFiles = qcDir.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    if (f.getName().startsWith(String.valueOf(id) + ".")) {
                        f.delete();
                    }
                }
            }
            setStatus(TaskI.State.FINISHED, runName + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteSeqRun.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }

    }
}
