package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
import java.io.File;
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
public class DeleteReference extends TaskI {

    private final long id;

    public DeleteReference(Connection conn, long id, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {
        List<Long> mappings = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM mapping WHERE ref_id=?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mappings.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete mappings
        for (Long mapId : mappings) {
            TaskI delJob = new DeleteMapping(mapId, conn, getProjectName());
            delJob.addPropertyChangeListener(this);
            delJob.run();
            delJob.removePropertyChangeListener(this);
        }

        try {
            String refName = null;
            String fileName;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    refName = rs.getString(1);
                    fileName = rs.getString(2);
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting reference " + refName);
            if (fileName != null) {
                File f = new File(fileName);
                if (f.exists()) {
                    f.delete();
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region WHERE ref_id=?")) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reference WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
            setStatus(TaskI.State.FINISHED, refName + " deleted");
        } catch (Exception e) {
            Logger.getLogger(DeleteReference.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
