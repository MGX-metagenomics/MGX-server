package de.cebitec.mgx.model.dao.deleteworkers;

import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sessions.TaskI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public DeleteSeqRun(long id, Connection conn, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {

        // fetch jobs for this seqrun
        List<Long> jobs = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM job WHERE seqrun_id=?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobs.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteSeqRun.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete jobs
        for (Long jobId : jobs) {
            TaskI delJob = new DeleteJob(jobId, conn, getProjectName());
            delJob.addPropertyChangeListener(this);
            delJob.run();
            delJob.removePropertyChangeListener(this);
        }


        try {
            String runName = null;
            String dBFile = null;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name, dbfile FROM seqrun WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    runName = rs.getString(1);
                    dBFile = rs.getString(2);
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting sequencing run " + runName);

            // remove persistent storage file
            if (dBFile != null) {
                SeqReaderFactory.delete(dBFile);
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }
            setStatus(TaskI.State.FINISHED, runName + " deleted");
        } catch (Exception e) {
            Logger.getLogger(DeleteSeqRun.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }

    }
}