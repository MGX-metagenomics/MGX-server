package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
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

/**
 *
 * @author sjaenick
 */
public final class DeleteSample extends TaskI {

    private final long id;
    private final File projectDir;
    private final MappingSessions mappingSessions;

    public DeleteSample(long id, GPMSManagedDataSourceI dataSource, String projName, File projectDir, MappingSessions mappingSessions) {
        super(projName, dataSource);
        this.id = id;
        this.projectDir = projectDir;
        this.mappingSessions = mappingSessions;
    }

    @Override
    public void process() {

        // fetch extracts for this sample
        List<Long> extracts = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM dnaextract WHERE sample_id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        extracts.add(rs.getLong(1));
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DeleteSample.class.getName()).log(Level.SEVERE, null, e);
            setStatus(TaskI.State.FAILED, e.getMessage());
            return;
        }

        // delete seqruns
        for (Long extId : extracts) {
            TaskI t = new DeleteDNAExtract(extId, getDataSource(), getProjectName(), projectDir, mappingSessions);
            t.addPropertyChangeListener(this);
            t.run();
            t.removePropertyChangeListener(this);
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
