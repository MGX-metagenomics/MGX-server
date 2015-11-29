package de.cebitec.mgx.global.worker;

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

    public InstallGlobalReference(Connection conn, MGXGlobal global, long globalId, String projRefDir, String projName) {
        super(projName, conn);
        this.global = global;
        this.projRefDir = projRefDir;
        this.globalId = globalId;
    }

    @Override
    public void run() {

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
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        // create reference in project to obtain an id
        long newRefId = -1;
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO reference (name, ref_length, ref_filepath) VALUES (?,?,?) RETURNING id")) {
            stmt.setString(1, globalRef.getName());
            stmt.setInt(2, globalRef.getLength());
            stmt.setString(3, "");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    newRefId = rs.getLong(1);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        setStatus(TaskI.State.PROCESSING, "Copying sequence");

        File targetFile = new File(projRefDir + newRefId + ".fas");
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
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE reference SET ref_filepath=? WHERE id=?")) {
            stmt.setString(1, targetFile.getAbsolutePath());
            stmt.setLong(2, newRefId);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        setStatus(TaskI.State.PROCESSING, "Copying subregions");

        // transfer regions
        Collection<Region> regions = null;
        try {
            regions = global.getRegionDAO().byReference(globalRef);
        } catch (MGXGlobalException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
        }

        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region (name, description, reg_start, reg_stop, ref_id) VALUES (?,?,?,?,?)")) {
            for (Region r : regions) {
                stmt.setString(1, r.getName());
                stmt.setString(2, r.getDescription());
                stmt.setInt(3, r.getStart());
                stmt.setInt(4, r.getStop());
                stmt.setLong(5, newRefId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        setStatus(TaskI.State.FINISHED, "Reference copied.");
    }
}
