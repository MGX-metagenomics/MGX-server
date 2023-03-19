/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.global;

import de.cebitec.mgx.common.ToolScope;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
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
public class GlobalToolDAO {

    private final MGXGlobal global;

    public GlobalToolDAO(MGXGlobal global) {
        this.global = global;
    }

    public Result<AutoCloseableIterator<Tool>> getAll() {
        List<Tool> tools = new ArrayList<>();
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, author, description, name, url, version, file, scope FROM tool")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Tool t = new Tool();
                        t.setId(rs.getLong(1));
                        t.setAuthor(rs.getString(2));
                        t.setDescription(rs.getString(3));
                        t.setName(rs.getString(4));
                        t.setUrl(rs.getString(5));
                        t.setVersion(rs.getFloat(6));
                        t.setFile(rs.getString(7));
                        t.setScope(ToolScope.values()[rs.getInt(8)]);
                        tools.add(t);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalToolDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(new ForwardingIterator<>(tools.iterator()));
    }

    public Result<Tool> getById(Long id) {
        Tool t = null;
        try (Connection conn = global.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT author, description, name, url, version, file, scope FROM tool WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        t = new Tool();
                        t.setId(id);
                        t.setAuthor(rs.getString(1));
                        t.setDescription(rs.getString(2));
                        t.setName(rs.getString(3));
                        t.setUrl(rs.getString(4));
                        t.setVersion(rs.getFloat(5));
                        t.setFile(rs.getString(6));
                        t.setScope(ToolScope.values()[rs.getInt(7)]);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(GlobalToolDAO.class.getName()).log(Level.SEVERE, null, ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(t);
    }

}
