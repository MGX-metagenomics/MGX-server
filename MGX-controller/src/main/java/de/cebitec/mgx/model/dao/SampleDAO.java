package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
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
public class SampleDAO extends DAO<Sample> {

    public SampleDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Sample.class;
    }

    private final static String CREATE = "INSERT INTO sample (collectiondate, material, temperature, volume, volume_unit, habitat_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Sample obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setDate(1, new java.sql.Date(obj.getCollectionDate().getTime()));
                stmt.setString(2, obj.getMaterial());
                stmt.setDouble(3, obj.getTemperature());
                stmt.setLong(4, obj.getVolume());
                stmt.setString(5, obj.getVolumeUnit());
                stmt.setLong(6, obj.getHabitatId());

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

    private final static String UPDATE = "UPDATE sample SET collectiondate=?, material=?, temperature=?, volume=?, volume_unit=?, habitat_id=?"
            + " WHERE id=?";

    public void update(Sample obj) throws MGXException {
        if (obj.getId() == Sample.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setDate(1, new java.sql.Date(obj.getCollectionDate().getTime()));
                stmt.setString(2, obj.getMaterial());
                stmt.setDouble(3, obj.getTemperature());
                stmt.setLong(4, obj.getVolume());
                stmt.setString(5, obj.getVolumeUnit());
                stmt.setLong(6, obj.getHabitatId());

                stmt.setLong(7, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public void delete(long id) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM sample WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private final static String BY_ID = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit, s.habitat_id "
            + "FROM sample s WHERE s.id=?";

    @Override
    public Sample getById(long id) throws MGXException {
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
                    Sample s = new Sample();
                    s.setId(rs.getLong(1));
                    s.setCollectionDate(new java.util.Date(rs.getDate(2).getTime()));
                    s.setMaterial(rs.getString(3));
                    s.setTemperature(rs.getDouble(4));
                    s.setVolume(rs.getInt(5));
                    s.setVolumeUnit(rs.getString(6));
                    s.setHabitatId(rs.getLong(7));
                    return s;
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private final static String FETCHALL = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit, s.habitat_id "
            + "FROM sample s";

    public AutoCloseableIterator<Sample> getAll() throws MGXException {
        List<Sample> ret = null;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {

                        if (ret == null) {
                            ret = new ArrayList<>();
                        }

                        Sample s = new Sample();
                        s.setId(rs.getLong(1));
                        s.setCollectionDate(new java.util.Date(rs.getDate(2).getTime()));
                        s.setMaterial(rs.getString(3));
                        s.setTemperature(rs.getDouble(4));
                        s.setVolume(rs.getInt(5));
                        s.setVolumeUnit(rs.getString(6));
                        s.setHabitatId(rs.getLong(7));

                        ret.add(s);

                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String SQL_BY_HABITAT = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit "
            + "FROM habitat h LEFT JOIN sample s ON (h.id=s.habitat_id) WHERE h.id=?";

    public AutoCloseableIterator<Sample> byHabitat(final long habitat_id) throws MGXException {
        if (habitat_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<Sample> ret = null;

//        Habitat habitat = getController().getHabitatDAO().getById(habitat_id);

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BY_HABITAT)) {
                stmt.setLong(1, habitat_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Habitat for ID " + habitat_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Sample s = new Sample();
                            s.setHabitatId(habitat_id);
                            s.setId(rs.getLong(1));
                            s.setCollectionDate(new java.util.Date(rs.getDate(2).getTime()));
                            s.setMaterial(rs.getString(3));
                            s.setTemperature(rs.getDouble(4));
                            s.setVolume(rs.getInt(5));
                            s.setVolumeUnit(rs.getString(6));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(s);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

//    public AutoCloseableIterator<Sample> byHabitat(Long habitat_id) throws MGXException {
//        Habitat h = getController().getHabitatDAO().getById(habitat_id);
//        Iterator<Sample> iterator = getEntityManager().<Sample>createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.habitat = :hab", Sample.class).
//                setParameter("hab", h).getResultList().iterator();
//        return new ForwardingIterator<>(iterator);
//    }
}
