package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.util.DBIterator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class ReferenceDAO<T extends Reference> extends DAO<T> {

    @Override
    public Class getType() {
        return Reference.class;
    }

    public String getSequence(final Reference ref, int from, int to) throws MGXException {
        if (from > to || from < 0 || to < 0 || from == to) {
            throw new MGXException("Invalid coordinates: " + from + " " + to);
        }
        int len = to-from+1;
        char[] buf = new char[len];
        try (BufferedReader br = new BufferedReader(new FileReader(ref.getFile()))) {
            br.readLine(); // skip header line
            br.skip(from);
            if (len != br.read(buf)) {
                throw new MGXException("Cannot retrieve sequence");
            }
            return String.valueOf(buf);
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    public DBIterator<Region> byReferenceInterval(final Reference ref, int from, int to) throws MGXException {

        if (from > to || from < 0 || to < 0 || from == to) {
            throw new MGXException("Invalid coordinates: " + from + " " + to);
        }
        DBIterator<Region> iter = null;
        Connection conn = getConnection();
        ResultSet rset;
        PreparedStatement stmt;

        try {
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
