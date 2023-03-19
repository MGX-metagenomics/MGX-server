package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteHabitat;
import java.io.IOException;
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
public class HabitatDAO extends DAO<Habitat> {

    public HabitatDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Habitat> getType() {
        return Habitat.class;
    }

    private final static String CREATE = "INSERT INTO habitat (name, biome, description, latitude, longitude) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Habitat obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getBiome());
                stmt.setString(3, obj.getDescription());
                stmt.setDouble(4, obj.getLatitude());
                stmt.setDouble(5, obj.getLongitude());

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

    private final static String UPDATE = "UPDATE habitat SET name=?, biome=?, description=?, latitude=?, "
            + "longitude=? WHERE id=?";

    public void update(Habitat obj) throws MGXException {
        if (obj.getId() == Habitat.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type Habitat without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getBiome());
                stmt.setString(3, obj.getDescription());
                stmt.setDouble(4, obj.getLatitude());
                stmt.setDouble(5, obj.getLongitude());

                stmt.setLong(6, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Habitat for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public TaskI delete(long id) throws MGXException, IOException {
        List<TaskI> subtasks = new ArrayList<>();
        try (AutoCloseableIterator<Sample> iter = getController().getSampleDAO().byHabitat(id)) {
            while (iter.hasNext()) {
                Sample s = iter.next();
                TaskI del = getController().getSampleDAO().delete(s.getId());
                subtasks.add(del);
            }
        }
        return new DeleteHabitat(getController().getDataSource(), id, getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
    }

//    public void delete(long id) throws MGXException {
//        try (Connection conn = getConnection()) {
//            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM habitat WHERE id=?")) {
//                stmt.setLong(1, id);
//                int numRows = stmt.executeUpdate();
//                if (numRows != 1) {
//                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
//                }
//            }
//        } catch (SQLException ex) {
//            throw new MGXException(ex);
//        }
//    }
    private static final String FETCHALL = "SELECT id, name, biome, description, latitude, longitude FROM habitat";
    private static final String BY_ID = "SELECT name, biome, description, latitude, longitude FROM habitat WHERE id=?";

    @Override
    public Habitat getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Habitat ret = new Habitat();
                        ret.setId(id);
                        ret.setName(rs.getString(1));
                        ret.setBiome(rs.getString(2));
                        ret.setDescription(rs.getString(3));
                        ret.setLatitude(rs.getDouble(4));
                        ret.setLongitude(rs.getDouble(5));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type Habitat for ID " + id + ".");
    }

    public AutoCloseableIterator<Habitat> getAll() throws MGXException {

        List<Habitat> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        Habitat ret = new Habitat();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setBiome(rs.getString(3));
                        ret.setDescription(rs.getString(4));
                        ret.setLatitude(rs.getDouble(5));
                        ret.setLongitude(rs.getDouble(6));
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
