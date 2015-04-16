/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.global;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class GlobalRegionDAO {

    private final MGXGlobal global;

    public GlobalRegionDAO(MGXGlobal global) {
        this.global = global;
    }

    Collection<Region> byReference(Reference ref) throws MGXException {
        Collection<Region> regions = new ArrayList<>();
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name, description, reg_start, reg_stop FROM region WHERE id=?")) {
                stmt.setLong(1, ref.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Region r = new Region();
                        r.setId(rs.getLong(1));
                        r.setName(rs.getString(2));
                        r.setDescription(rs.getString(3));
                        r.setStart(rs.getInt(4));
                        r.setStop(rs.getInt(5));
                        r.setReference(ref);
                        regions.add(r);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalRegionDAO.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
        return regions;
    }

}
