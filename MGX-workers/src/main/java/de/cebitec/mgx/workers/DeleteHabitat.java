package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.TaskI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public final class DeleteHabitat extends TaskI {

    private final long id;
    private final TaskI[] subtasks;

    public DeleteHabitat(GPMSManagedDataSourceI dataSource, long id, String projName, TaskI... subtasks) {
        super(projName, dataSource);
        this.id = id;
        this.subtasks = subtasks;
    }

    @Override
    public void process() {

        for (TaskI subtask : subtasks) {
            subtask.addPropertyChangeListener(this);
            subtask.run();
            subtask.removePropertyChangeListener(this);
        }

        try {
            String habitatName = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM habitat WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            habitatName = rs.getString(1);
                        }
                    }
                }
            }
            if (habitatName != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting habitat " + habitatName);
            }

            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM habitat WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
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
