package de.cebitec.mgx.model.dao.deleteworkers;

import de.cebitec.mgx.sessions.TaskI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public final class DeleteJob extends TaskI {

    private final long id;

    public DeleteJob(long id, Connection conn, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {
        try {

            // delete observations
            setStatus(TaskI.State.PROCESSING, "Deleting observations");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // delete attributecounts
            setStatus(TaskI.State.PROCESSING, "Deleting attributes");
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id=?)")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            // delete attributes
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            setStatus(TaskI.State.PROCESSING, "Deleting job " + id);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM job WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }

            setStatus(TaskI.State.FINISHED, "Job " + id + " deleted");
        } catch (Exception e) {
            Logger.getLogger(DeleteJob.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}