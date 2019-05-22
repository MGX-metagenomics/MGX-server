package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sj
 */
public class ReferenceDAO extends DAO<Reference> {

    public ReferenceDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    public Class getType() {
        return Reference.class;
    }

    @Override
    public long create(Reference obj) throws MGXException {
        AutoCloseableIterator<Reference> iter = getAll();
        while (iter.hasNext()) {
            Reference ref = iter.next();
            if (ref.getName().equals(obj.getName())) {
                throw new MGXException("A reference named " + obj.getName() + " already exists.");
            }
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO reference (name, ref_length, ref_filepath) VALUES (?,?,?) RETURNING id")) {
                stmt.setString(1, obj.getName());
                stmt.setInt(2, obj.getLength());
                stmt.setString(3, obj.getFile());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    private final static String UPDATE = "UPDATE reference SET name=?, ref_length=? WHERE id=?";

    public void update(Reference obj) throws MGXException {
        if (obj.getId() == Reference.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setInt(2, obj.getLength());

                stmt.setLong(3, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public TaskI delete(long id) throws MGXException, IOException {
        List<TaskI> subtasks = new ArrayList<>();
        try(AutoCloseableIterator<Mapping> iter = getController().getMappingDAO().byReference(id)) {
             while (iter.hasNext()) {
                Mapping s = iter.next();
                TaskI del = getController().getMappingDAO().delete(s.getId(), getController().getDataSource(),
                        getController().getProjectName(), getController().getProjectJobDirectory().getAbsolutePath());
                subtasks.add(del);
            }
        }
        return new DeleteReference(id, 
                getController().getDataSource(), 
                getController().getProjectName(), 
                getController().getProjectJobDirectory().getAbsolutePath(),
                subtasks.toArray(new TaskI[]{}));
    }

    private final static String BY_ID = "SELECT id, name, ref_length, ref_filepath FROM reference WHERE id=?";

    @Override
    public Reference getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }
                    Reference ref = new Reference();
                    ref.setId(rs.getLong(1));
                    ref.setName(rs.getString(2));
                    ref.setLength(rs.getInt(3));
                    ref.setFile(rs.getString(4));

                    if (ref.getFile() != null) {
                        File seqFile = new File(ref.getFile());
                        if (!(seqFile.exists() && seqFile.canRead())) {
                            getController().log("Reference sequence data file " + ref.getFile() + " for " + ref.getName() + " is missing or unreadable.");
                            throw new MGXException("Reference sequence data file for " + ref.getName() + " is missing or unreadable.");
                        }
                    }
                    return ref;
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public AutoCloseableIterator<Reference> getAll() throws MGXException {
        List<Reference> refs = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name, ref_length, ref_filepath FROM reference")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Reference ref = new Reference();
                        ref.setId(rs.getLong(1));
                        ref.setName(rs.getString(2));
                        ref.setLength(rs.getInt(3));
                        ref.setFile(rs.getString(4));

                        if (ref.getFile() != null) {
                            File seqFile = new File(ref.getFile());
                            if (!(seqFile.exists() && seqFile.canRead())) {
                                getController().log("Reference sequence data file " + ref.getFile() + " for " + ref.getName() + " is missing or unreadable.");
                                throw new MGXException("Reference sequence data file for " + ref.getName() + " is missing or unreadable.");
                            }
                        }

                        refs.add(ref);
                    }
                }
            }

        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(refs.iterator());
    }

    public String getSequence(long refId, int from, int to) throws MGXException {
        int refLen = -1;
        String filePath = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT ref_length, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, refId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        refLen = rs.getInt(1);
                        filePath = rs.getString(2);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
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
            getController().log(ex);
            throw new MGXException(ex.getMessage());
        }
    }

    public DBIterator<Region> byReferenceInterval(final long refId, final Reference ref, int from, int to) throws MGXException {

        if (from > to || from < 0 || to < 0 || from == to || to > ref.getLength()) {
            throw new MGXException("Invalid coordinates: " + from + " " + to);
        }
        DBIterator<Region> iter = null;
        ResultSet rset;
        PreparedStatement stmt;

        try {
            Connection conn = getConnection();
            stmt = conn.prepareStatement("SELECT * from getRegions(?,?,?)");
            stmt.setLong(1, refId);
            stmt.setInt(2, from);
            stmt.setInt(3, to);
            rset = stmt.executeQuery();

            iter = new DBIterator<Region>(rset, stmt, conn) {
                @Override
                public Region convert(ResultSet rs) throws SQLException {
                    Region r = new Region();
                    r.setReferenceId(refId);
                    r.setId(rs.getLong(1));
                    r.setName(rs.getString(2));
                    r.setType(rs.getString(3));
                    r.setDescription(rs.getString(4));
                    r.setStart(rs.getInt(5));
                    r.setStop(rs.getInt(6));
                    return r;
                }
            };

        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex.getMessage());
        }
        return iter;
    }
}
