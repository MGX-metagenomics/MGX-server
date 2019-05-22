package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.TaskI;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class DeleteMapping extends TaskI {

    private final long id;
    private final String projectJobDir;

    public DeleteMapping(long mapId, GPMSManagedDataSourceI dataSource, String projName, String jobDir) {
        super(projName, dataSource);
        id = mapId;
        this.projectJobDir = jobDir;
    }

    @Override
    public void process() {

        TLongSet jobs = new TLongHashSet();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mapping WHERE id=? RETURNING bam_file, job_id")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String bamName = rs.getString(1);
                        File bamFile = new File(bamName);
                        if (bamFile.exists()) {
                            bamFile.delete();
                        }
                        File bamIdx = new File(bamName + ".bai");
                        if (bamIdx.exists()) {
                            bamIdx.delete();
                        }
                        File covFile = new File(bamName + ".maxCov");
                        if (covFile.exists()) {
                            covFile.delete();
                        }
                        jobs.add(rs.getLong(2));
                    }
                }

                // delete jobs
                jobs.forEach(new TLongProcedure() {
                    @Override
                    public boolean execute(long jobId) {
                        TaskI delJob = new DeleteJob(jobId, getDataSource(), getProjectName(), projectJobDir);
                        delJob.addPropertyChangeListener(DeleteMapping.this);
                        delJob.run();
                        delJob.removePropertyChangeListener(DeleteMapping.this);
                        return true;
                    }
                });
            }
        } catch (SQLException ex) {
            Logger.getLogger(DeleteMapping.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(TaskI.State.FAILED, ex.getMessage());
        }
    }
}
