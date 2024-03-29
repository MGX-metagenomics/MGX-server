/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.global;

import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
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
public class GlobalReferenceDAO {

    private final MGXGlobal global;

    public GlobalReferenceDAO(MGXGlobal global) {
        this.global = global;
    }

    public Result<Reference> getById(Long id) {
        Reference ref = null;
        try ( Connection conn = global.getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT name, ref_length, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ref = new Reference();
                        ref.setId(id);
                        ref.setName(rs.getString(1));
                        ref.setLength(rs.getInt(2));
                        ref.setFile(rs.getString(3));
                    }
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(GlobalReferenceDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }

        if (ref != null) {
            File seqFile = new File(ref.getFile());
            if (!(seqFile.exists() && seqFile.canRead())) {
                global.log("Global reference sequence data file " + ref.getFile() + " for " + ref.getName() + " is missing or unreadable.");
                return Result.error("Global reference sequence data file for " + ref.getName() + " is missing or unreadable.");
            }
        }
        return Result.ok(ref);
    }

    public Result<AutoCloseableIterator<Reference>> getAll() {
        List<Reference> refs = new ArrayList<>();
        try ( Connection conn = global.getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT id, name, ref_length, ref_filepath FROM reference")) {
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Reference ref = new Reference();
                        ref.setId(rs.getLong(1));
                        ref.setName(rs.getString(2));
                        ref.setLength(rs.getInt(3));
                        ref.setFile(rs.getString(4));

                        File seqFile = new File(ref.getFile());
                        if (!(seqFile.exists() && seqFile.canRead())) {
                            global.log("Global reference sequence data file " + ref.getFile() + " for " + ref.getName() + " is missing or unreadable.");
                            return Result.error("Global reference sequence data file for " + ref.getName() + " is missing or unreadable.");
                        }

                        refs.add(ref);
                    }
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(GlobalReferenceDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(new ForwardingIterator<>(refs.iterator()));
    }

}
