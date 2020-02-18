package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
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
public final class DeleteSeqRun extends TaskI {

    private final long id;
    private final File projectDir;
    private final String dbFile;

    public DeleteSeqRun(long id, GPMSManagedDataSourceI dataSource, String projName, File projectDir, String dbFile) {
        super(projName, dataSource);
        this.id = id;
        this.projectDir = projectDir;
        this.dbFile = dbFile;
    }

    @Override
    public void process() {

        // set number of sequences to -1 so this seqrun isn't returned
        // in getAll() / ByDNAExtract()
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE seqrun SET num_sequences=-1 WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteSeqRun.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // fetch jobs for this seqrun
        List<Long> jobs = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT j.id FROM job j WHERE ?=ANY(j.seqruns)")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (jobs == null) {
                            jobs = new ArrayList<>();
                        }
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
        if (jobs != null) {
            for (Long jobId : jobs) {
                TaskI delJob = new DeleteJob(jobId, getDataSource(), getProjectName(), projectDir + File.separator + "jobs");
                delJob.addPropertyChangeListener(this);
                delJob.run();
                delJob.removePropertyChangeListener(this);
            }
        }

        try {
            String runName = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM seqrun WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            runName = rs.getString(1);
                        }
                    }
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting sequencing run " + runName);

            // remove persistent storage file
            if (dbFile != null) {
                SeqReaderFactory.delete(dbFile);
            }

            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
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
