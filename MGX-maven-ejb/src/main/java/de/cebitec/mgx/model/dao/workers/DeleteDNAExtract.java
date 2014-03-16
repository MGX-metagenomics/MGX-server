package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
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
public final class DeleteDNAExtract extends TaskI {

    private final long id;

    public DeleteDNAExtract(long id, Connection conn, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {

        // fetch seqruns for this extract
        List<Long> seqruns = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM seqrun WHERE dnaextract_id=?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    seqruns.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            Logger.getLogger(DeleteDNAExtract.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete seqruns
        for (Long runId : seqruns) {
            TaskI delRun = new DeleteSeqRun(runId, conn, getProjectName());
            delRun.addPropertyChangeListener(this);
            delRun.run();
            delRun.removePropertyChangeListener(this);
        }

        try {
            String extractName = null;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM dnaextract WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        extractName = rs.getString(1);
                    }
                }
            }

            if (extractName != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting DNA extract " + extractName);
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM dnaextract WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }
            if (extractName != null) {
                setStatus(TaskI.State.FINISHED, extractName + " deleted");
            }
        } catch (SQLException e) {
            Logger.getLogger(DeleteDNAExtract.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
