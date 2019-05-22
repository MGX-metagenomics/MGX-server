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
public final class DeleteBin extends TaskI {

    private final long id;
    private final TaskI[] subtasks;

    public DeleteBin(GPMSManagedDataSourceI dataSource, long id, String projName, TaskI... subtasks) {
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
            String name = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM bin WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            name = rs.getString(1);
                        }
                    }
                }
            }
            if (name != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting bin " + name);
            }

            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM bin WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }
            if (name != null) {
                setStatus(TaskI.State.FINISHED, name + " deleted");
            }
        } catch (SQLException e) {
            Logger.getLogger(DeleteBin.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
