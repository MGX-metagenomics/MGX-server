package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.DBIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sj
 */
public class ReferenceDAO<T extends Reference> extends DAO<T> {

    public ReferenceDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    public Class getType() {
        return Reference.class;
    }

    public String getSequence(long refId, int from, int to) throws MGXException {
        int refLen = -1;
        String filePath = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT ref_length, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, refId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        refLen = rs.getInt(1);
                        filePath = rs.getString(2);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (refLen == -1 || filePath == null) {
            throw new MGXException("Cannot read data for project reference id " + refId);
        }

        if (!new File(filePath).exists()) {
            throw new MGXException("Sequence data file for ID " + refId + " is missing.");
        }

        if (from > to || from < 0 || to < 0 || from == to || to > refLen) {
            throw new MGXException("Invalid coordinates: " + from + " " + to + ", reference length is " + refLen);
        }
        int len = to - from + 1;
        char[] buf = new char[len];
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // skip header line
            br.skip(from);
            if (len != br.read(buf)) {
                throw new MGXException("Cannot retrieve sequence");
            }
            return String.valueOf(buf).toUpperCase();
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    public DBIterator<Region> byReferenceInterval(final Reference ref, int from, int to) throws MGXException {

        if (from > to || from < 0 || to < 0 || from == to || to > ref.getLength()) {
            throw new MGXException("Invalid coordinates: " + from + " " + to);
        }
        DBIterator<Region> iter = null;
        ResultSet rset;
        PreparedStatement stmt;

        try {
            Connection conn = getConnection();
            stmt = conn.prepareStatement("SELECT * from getRegions(?,?,?)");
            stmt.setLong(1, ref.getId());
            stmt.setInt(2, from);
            stmt.setInt(3, to);
            rset = stmt.executeQuery();

            iter = new DBIterator<Region>(rset, stmt, conn) {
                @Override
                public Region convert(ResultSet rs) throws SQLException {
                    Region r = new Region();
                    r.setReference(ref);
                    r.setId(rs.getLong(1));
                    r.setName(rs.getString(2));
                    r.setDescription(rs.getString(3));
                    r.setStart(rs.getInt(4));
                    r.setStop(rs.getInt(5));
                    return r;
                }
            };

        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }
}
