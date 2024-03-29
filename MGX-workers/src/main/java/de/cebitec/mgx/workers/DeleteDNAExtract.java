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
public final class DeleteDNAExtract extends TaskI {

    private final long id;
    private final TaskI[] subtasks;

    public DeleteDNAExtract(long id, GPMSManagedDataSourceI dataSource, String projName, TaskI... subtasks) {
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
            String extractName = null;
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM dnaextract WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            extractName = rs.getString(1);
                        }
                    }
                }
            }

            if (extractName != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting DNA extract " + extractName);
            }
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM dnaextract WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
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
