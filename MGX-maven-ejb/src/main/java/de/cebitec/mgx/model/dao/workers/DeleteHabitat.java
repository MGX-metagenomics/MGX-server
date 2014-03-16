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
public final class DeleteHabitat extends TaskI {

    private final long id;

    public DeleteHabitat(Connection conn, long id, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {

        // fetch samples for this habitat
        List<Long> samples = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM sample WHERE habitat_id=?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    samples.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteHabitat.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete samples
        for (Long sampleId : samples) {
            TaskI t = new DeleteSample(sampleId, conn, getProjectName());
            t.addPropertyChangeListener(this);
            t.run();
            t.removePropertyChangeListener(this);
        }

        try {
            String habitatName = null;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM habitat WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        habitatName = rs.getString(1);
                    }
                }
            }
            if (habitatName != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting habitat " + habitatName);
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM habitat WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }
            if (habitatName != null) {
                setStatus(TaskI.State.FINISHED, habitatName + " deleted");
            }
        } catch (SQLException e) {
            Logger.getLogger(DeleteHabitat.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
