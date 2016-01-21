package de.cebitec.mgx.workers;

import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.core.TaskI;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class DeleteReference extends TaskI {

    private final long id;
    private final MappingSessions sessions;

    public DeleteReference(DataSource dataSource, long id, String projName, MappingSessions sessions) {
        super(projName, dataSource);
        this.id = id;
        this.sessions = sessions;
    }

    @Override
    public void run() {
        List<Long> mappings = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM mapping WHERE ref_id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        mappings.add(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete mappings
        for (Long mapId : mappings) {
            TaskI delJob = new DeleteMapping(mapId, dataSource, getProjectName(), sessions);
            delJob.addPropertyChangeListener(this);
            delJob.run();
            delJob.removePropertyChangeListener(this);
        }

        try {
            String refName = null;
            String fileName = null;
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name, ref_filepath FROM reference WHERE id=?")) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            refName = rs.getString(1);
                            fileName = rs.getString(2);
                        }
                    }
                }
            }
            if (refName != null) {
                setStatus(TaskI.State.PROCESSING, "Deleting reference " + refName);
            }
            if (fileName != null) {
                File f = new File(fileName);
                if (f.exists()) {
                    f.delete();
                }
            }

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region WHERE ref_id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reference WHERE id=?")) {
                    stmt.setLong(1, id);
                    stmt.executeUpdate();
                }
            }

            setStatus(TaskI.State.FINISHED, refName + " deleted");
        } catch (SQLException e) {
            Logger.getLogger(DeleteReference.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }
    }
}
