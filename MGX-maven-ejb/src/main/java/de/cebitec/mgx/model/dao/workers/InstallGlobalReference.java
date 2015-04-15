/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.sessions.TaskI;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
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
public class InstallGlobalReference extends TaskI {

    private final String projRefDir;
    private final long globalId;
    private final String refName;
    private final int refLength;
    private final File refData;
    private final Connection globalConn;

    public InstallGlobalReference(Connection conn, Connection globalConn, Reference globalRef, String projRefDir, String projName) {
        super(projName, conn);
        this.globalConn = globalConn;
        this.projRefDir = projRefDir;
        this.globalId = globalRef.getId();
        this.refName = globalRef.getName();
        this.refLength = globalRef.getLength();
        this.refData = new File(globalRef.getFile());
    }

    @Override
    public void run() {

        File referencesDir = new File(projRefDir);
        if (!referencesDir.exists()) {
            UnixHelper.createDirectory(referencesDir);
        }

        // create reference in project to obtain an id
        long newRefId = -1;
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO reference (name, ref_length, ref_filepath) VALUES (?,?,?) RETURNING id")) {
            stmt.setString(1, refName);
            stmt.setInt(2, refLength);
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
            UnixHelper.copyFile(refData, targetFile);
        } catch (IOException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, "Could not copy DNA sequence");
            return;
        } finally {
            if (targetFile.exists()) {
                targetFile.delete();
            }
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
        try (PreparedStatement gStmt = globalConn.prepareStatement("SELECT name, description, reg_start, reg_stop FROM region WHERE id=?")) {
            gStmt.setLong(1, globalId);
            try (ResultSet rs = gStmt.executeQuery()) {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region (name, description, reg_start, reg_stop, ref_id) VALUES (?,?,?,?,?)")) {

                    while (rs.next()) {
                        String name = rs.getString(1);
                        String desc = rs.getString(2);
                        int start = rs.getInt(3);
                        int stop = rs.getInt(4);

                        stmt.setString(1, name);
                        stmt.setString(2, desc);
                        stmt.setInt(3, start);
                        stmt.setInt(4, stop);
                        stmt.setLong(5, newRefId);
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(InstallGlobalReference.class.getName()).log(Level.SEVERE, null, ex);
            setStatus(State.FAILED, ex.getMessage());
            return;
        }

        setStatus(TaskI.State.FINISHED, "Reference copied.");
        
        try {
            globalConn.close();
        } catch (SQLException ex) {
        }

    }

    @Override
    public void close() {
        if (globalConn != null) {
            try {
                globalConn.close();
            } catch (SQLException ex) {
            }
        }
        super.close();
    }

    @Override
    public void cancel() {
        if (globalConn != null) {
            try {
                globalConn.close();
            } catch (SQLException ex) {
            }
        }
        super.cancel();
    }

}
