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
public final class DeleteAssembly extends TaskI {

    private final long id;
    private final TaskI[] subtasks;

    public DeleteAssembly(GPMSManagedDataSourceI dataSource, long id, String projName, TaskI... subtasks) {
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
            long asmJobId = -1;
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("SELECT name, job_id FROM assembly WHERE id=?")) {
                    stmt.setLong(1, id);
                    try ( ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            name = rs.getString(1);
                            asmJobId = rs.getLong(2);
                        }
                    }
                }
            }
            
            if (asmJobId == -1) {
                setStatus(TaskI.State.FAILED, "No such assembly.");
                return;
            }
            
            if (name != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting assembly " + name);
            }

            // delete the assembly itself
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM assembly WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.execute();
                }
            }

            //
            // delete the job (and its parameters) that created this assembly
            //
            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM jobparameter WHERE job_id=?")) {
                    stmt.setLong(1, asmJobId);
                    stmt.executeUpdate();
                }
            }

            try ( Connection conn = getConnection()) {
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM job WHERE id=?")) {
                    stmt.setLong(1, asmJobId);
                    stmt.executeUpdate();
                }
            }

            if (name != null) {
                setStatus(TaskI.State.FINISHED, name + " deleted");
            }
        } catch (SQLException e) {
            Logger.getLogger(DeleteAssembly.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
