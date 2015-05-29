/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.global;

import de.cebitec.mgx.controller.MGXException;
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

    public Reference getById(Long id) throws MGXException {
        Reference ref = null;
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name, ref_length, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException(ex);
        }

        if (ref != null) {
            File seqFile = new File(ref.getFile());
            if (!seqFile.exists()) {
                throw new MGXException("Reference sequence data for " + ref.getName() + " is missing.");
            }
        }
        return ref;
    }

    public AutoCloseableIterator<Reference> getAll() throws MGXException {
        List<Reference> refs = new ArrayList<>();
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name, ref_length, ref_filepath FROM reference")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Reference ref = new Reference();
                        ref.setId(rs.getLong(1));
                        ref.setName(rs.getString(2));
                        ref.setLength(rs.getInt(3));
                        ref.setFile(rs.getString(4));
                        refs.add(ref);
                    }
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(GlobalReferenceDAO.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(refs.iterator());
    }

}
