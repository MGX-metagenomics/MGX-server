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
public final class DeleteSample extends TaskI {

    private final long id;
    private final TaskI[] subtasks;

    public DeleteSample(long id, GPMSManagedDataSourceI dataSource, String projName, TaskI[] subtasks) {
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
            String sampleName = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT material FROM sample WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            sampleName = rs.getString(1);
                        }
                    }
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting sample " + sampleName);

            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM sample WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }
            setStatus(TaskI.State.FINISHED, sampleName + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteSample.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }

    }
}
