package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteAssembly;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class AssemblyDAO extends DAO<Assembly> {

    public AssemblyDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Assembly.class;
    }

    private final static String CREATE = "INSERT INTO assembly (name, reads_assembled, n50, job_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Assembly obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getReadsAssembled());
                stmt.setLong(3, obj.getN50());
                stmt.setLong(4, obj.getAsmjobId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    private final static String UPDATE = "UPDATE assembly SET name=?, reads_assembled=?, n50=?, job_id=? WHERE id=?";

    public void update(Assembly obj) throws MGXException {
        if (obj.getId() == Assembly.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getReadsAssembled());
                stmt.setLong(3, obj.getN50());
                stmt.setLong(4, obj.getAsmjobId());

                stmt.setLong(5, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public TaskI delete(long id) throws MGXException {
        List<TaskI> subtasks = new ArrayList<>();
        try (AutoCloseableIterator<Bin> iter = getController().getBinDAO().byAssembly(id)) {
            while (iter.hasNext()) {
                Bin s = iter.next();
                TaskI del = getController().getBinDAO().delete(s.getId());
                subtasks.add(del);
            }
        }
        return new DeleteAssembly(getController().getDataSource(), id, getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
    }

    private static final String FETCHALL = "SELECT id, name, reads_assembled, n50, job_id FROM assembly";
    private static final String BY_ID = "SELECT id, name, reads_assembled, n50, job_id FROM assembly WHERE id=?";

    @Override
    public Assembly getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Assembly ret = new Assembly();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setReadsAssembled(rs.getLong(3));
                        ret.setN50(rs.getInt(4));
                        ret.setAsmjobId(rs.getLong(5));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
    }

    public AutoCloseableIterator<Assembly> getAll() throws MGXException {

        List<Assembly> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        Assembly ret = new Assembly();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setReadsAssembled(rs.getLong(3));
                        ret.setN50(rs.getInt(4));
                        ret.setAsmjobId(rs.getLong(5));

                        if (l == null) {
                            l = new ArrayList<>();
                        }
                        l.add(ret);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(l == null ? null : l.iterator());
    }

}
