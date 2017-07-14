package de.cebitec.mgx.global.worker;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.MGXGlobalException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class InstallGlobalReference extends TaskI {

    private final String projRefDir;
    private final long globalId;
    private final MGXGlobal global;

    public InstallGlobalReference(GPMSManagedDataSourceI projectDataSource, MGXGlobal global, long globalId, String projRefDir, String projName) {
        super(projName, projectDataSource);
        this.global = global;
        this.projRefDir = projRefDir;
        this.globalId = globalId;
    }

    @Override
    public void process() {

        File referencesDir = new File(projRefDir);
        if (!referencesDir.exists()) {
            try {
                UnixHelper.createDirectory(referencesDir);
            } catch (IOException ex) {
                setStatus(State.FAILED, ex.getMessage());
                return;
            }
        }

        Reference globalRef;
        try {
            globalRef = global.getReferenceDAO().getById(globalId);
        } catch (MGXGlobalException ex) {
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        // create reference in project to obtain an id
        long newRefId = -1;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO reference (name, ref_length, ref_filepath) VALUES (?,?,?) RETURNING id")) {
                stmt.setString(1, globalRef.getName());
                stmt.setInt(2, globalRef.getLength());
                stmt.setString(3, "");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        newRefId = rs.getLong(1);
                    } else {
                        setStatus(State.FAILED, "Error creating reference");
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        setStatus(TaskI.State.PROCESSING, "Copying sequence");

        File targetFile = new File(referencesDir, newRefId + ".fas");
        try {
            UnixHelper.copyFile(new File(globalRef.getFile()), targetFile);
        } catch (IOException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, "Could not copy DNA sequence");
            if (targetFile.exists()) {
                targetFile.delete();
            }
            return;
        }

        // update filepath on reference
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE reference SET ref_filepath=? WHERE id=?")) {
                stmt.setString(1, targetFile.getAbsolutePath());
                stmt.setLong(2, newRefId);
                stmt.execute();
            }
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());

            // cleanup attempt
            delReference(newRefId);

            return;
        }

        setStatus(TaskI.State.PROCESSING, "Copying subregions");

        // transfer regions
        // TODO: convert to iterator
        Collection<Region> regions;
        try {
            regions = global.getRegionDAO().byReference(globalId);
        } catch (MGXGlobalException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());

            // cleanup attempt
            delReference(newRefId);

            return;
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region (name, type, description, reg_start, reg_stop, ref_id) VALUES (?,?,?,?,?,?)")) {
                for (Region r : regions) {
                    stmt.setString(1, r.getName());
                    stmt.setString(2, r.getType());
                    stmt.setString(3, r.getDescription());
                    stmt.setInt(4, r.getStart());
                    stmt.setInt(5, r.getStop());
                    stmt.setLong(6, newRefId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());

            // cleanup attempt
            delRegions(newRefId);
            delReference(newRefId);
            return;
        }

        setStatus(TaskI.State.FINISHED, "Reference copied.");
    }

    private void delRegions(long refId) {
        if (refId != -1) {
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM region WHERE ref_id=?")) {
                    stmt.setLong(1, refId);
                    stmt.execute();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    private void delReference(long refId) {
        if (refId != -1) {
            // cleanup attempt
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reference WHERE id=?")) {
                    stmt.setLong(1, refId);
                    stmt.execute();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }
}
