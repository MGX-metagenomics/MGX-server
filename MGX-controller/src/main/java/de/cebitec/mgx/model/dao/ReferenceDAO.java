package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
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
    public Class<Reference> getType() {
        return Reference.class;
    }

    @Override
    public long create(Reference obj) throws MGXException {
        Result<AutoCloseableIterator<Reference>> res = getAll();
        if (res.isError()) {
            throw new MGXException(res.getError());
        }

        // check for duplicate name
        try ( AutoCloseableIterator<Reference> iter = res.getValue()) {
            while (iter.hasNext()) {
                Reference ref = iter.next();
                if (ref.getName().equals(obj.getName())) {
                    throw new MGXException("A reference named " + obj.getName() + " already exists.");
                }
            }
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("INSERT INTO reference (name, ref_length, ref_filepath) VALUES (?,?,?) RETURNING id")) {
                stmt.setString(1, obj.getName());
                stmt.setInt(2, obj.getLength());
                stmt.setString(3, obj.getFile());
                try ( ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException("Cannot update object of type Reference without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setInt(2, obj.getLength());

                stmt.setLong(3, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Reference for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public Result<TaskI> delete(long id) throws IOException {

        Result<AutoCloseableIterator<Mapping>> res = getController().getMappingDAO().byReference(id);
        if (res.isError()) {
            return Result.error(res.getError());
        }

        List<TaskI> subtasks = new ArrayList<>();
        try ( AutoCloseableIterator<Mapping> iter = res.getValue()) {
            while (iter.hasNext()) {
                Mapping s = iter.next();
                Result<TaskI> del = getController().getMappingDAO().delete(s.getId(), getController().getDataSource(),
                        getController().getProjectName(), getController().getProjectJobDirectory().getAbsolutePath());
                if (del.isError()) {
                    return Result.error(del.getError());
                }
                subtasks.add(del.getValue());
            }
        }
        return Result.ok(new DeleteReference(id,
                getController().getDataSource(),
                getController().getProjectName(),
                getController().getProjectJobDirectory().getAbsolutePath(),
                subtasks.toArray(new TaskI[]{})));
    }

    private final static String BY_ID = "SELECT id, name, ref_length, ref_filepath FROM reference WHERE id=?";

    @Override
    public Result<Reference> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type Reference for ID " + id + ".");
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
                            return Result.error("Reference sequence data file for " + ref.getName() + " is missing or unreadable.");
                        }
                    }
                    return Result.ok(ref);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<AutoCloseableIterator<Reference>> getAll() {
        List<Reference> refs = new ArrayList<>();
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT id, name, ref_length, ref_filepath FROM reference")) {
                try ( ResultSet rs = stmt.executeQuery()) {
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
                                return Result.error("Reference sequence data file for " + ref.getName() + " is missing or unreadable.");
                            }
                        }

                        refs.add(ref);
                    }
                }
            }

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        return Result.ok(new ForwardingIterator<>(refs.iterator()));
    }

    public Result<String> getSequence(long refId, int from, int to) {
        int refLen = -1;
        String filePath;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT ref_length, ref_filepath FROM reference WHERE id=?")) {
                stmt.setLong(1, refId);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type Reference for ID " + refId + ".");
                    }
                    refLen = rs.getInt(1);
                    filePath = rs.getString(2);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        if (refLen == -1 || filePath == null) {
            return Result.error("Cannot read data for project reference id " + refId);
        }

        if (!new File(filePath).exists()) {
            return Result.error("Sequence data file for ID " + refId + " is missing.");
        }

        if (from > to || from < 0 || to < 0 || from == to || to > refLen) {
            return Result.error("Invalid coordinates: " + from + " " + to + ", reference length is " + refLen);
        }
        int len = to - from + 1;
        char[] buf = new char[len];
        try ( BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // skip header line
            br.skip(from);
            if (len != br.read(buf)) {
                return Result.error("Cannot retrieve sequence");
            }
            return Result.ok(String.valueOf(buf).toUpperCase());
        } catch (IOException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }
}
