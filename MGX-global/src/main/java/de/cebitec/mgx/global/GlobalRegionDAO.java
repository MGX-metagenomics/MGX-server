/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.global;

import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.ReferenceRegion;
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

    public Result<Collection<ReferenceRegion>> byReference(long refId) {
        Collection<ReferenceRegion> regions = new ArrayList<>();
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name, description, reg_start, reg_stop, type FROM region WHERE ref_id=?")) {
                stmt.setLong(1, refId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ReferenceRegion r = new ReferenceRegion();
                        
                        r.setReferenceId(refId);
                        
                        r.setId(rs.getLong(1));
                        r.setName(rs.getString(2));
                        r.setDescription(rs.getString(3));
                        r.setStart(rs.getInt(4));
                        r.setStop(rs.getInt(5));

                        String type = rs.getString(6);
                        switch (type) {
                            case "CDS":
                                r.setType(RegionType.CDS);
                                break;
                            case "tRNA":
                                r.setType(RegionType.TRNA);
                                break;
                            case "rRNA":
                                r.setType(RegionType.RRNA);
                                break;
                            case "tmRNA":
                                r.setType(RegionType.TMRNA);
                                break;
                            case "ncRNA":
                                r.setType(RegionType.NCRNA);
                                break;
                            default:
                                r.setType(RegionType.MISC);

                        }
                        //r.setReference(ref);
                        regions.add(r);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalRegionDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(regions);
    }

}
