package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
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
public final class DeleteSample extends TaskI {

    private final long id;

    public DeleteSample(long id, Connection conn, String projName) {
        super(projName, conn);
        this.id = id;
    }

    @Override
    public void run() {

        // fetch extracts for this sample
        List<Long> extracts = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM dnaextract WHERE sample_id=?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    extracts.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteSample.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete seqruns
        for (Long extId : extracts) {
            TaskI t = new DeleteDNAExtract(extId, conn, getProjectName());
            t.addPropertyChangeListener(this);
            t.run();
            t.removePropertyChangeListener(this);
        }


        try {
            String sampleName = null;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT material FROM sample WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    sampleName = rs.getString(1);
                }
            }
            setStatus(TaskI.State.PROCESSING, "Deleting sample " + sampleName);

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM sample WHERE id=?")) {
                stmt.setLong(1, id);
                stmt.execute();
            }
            setStatus(TaskI.State.FINISHED, sampleName + " deleted");
        } catch (Exception e) {
            Logger.getLogger(DeleteSample.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
        }

    }
}