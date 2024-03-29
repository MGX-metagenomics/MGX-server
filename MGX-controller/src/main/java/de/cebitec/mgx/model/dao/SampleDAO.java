package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.workers.DeleteSample;
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
public class SampleDAO extends DAO<Sample> {

    public SampleDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Sample> getType() {
        return Sample.class;
    }

    private final static String CREATE = "INSERT INTO sample (collectiondate, material, temperature, volume, volume_unit, habitat_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Sample obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setDate(1, new java.sql.Date(obj.getCollectionDate().getTime()));
                stmt.setString(2, obj.getMaterial());
                stmt.setDouble(3, obj.getTemperature());
                stmt.setLong(4, obj.getVolume());
                stmt.setString(5, obj.getVolumeUnit());
                stmt.setLong(6, obj.getHabitatId());

                try ( ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException("Cannot update object of type Sample without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setDate(1, new java.sql.Date(obj.getCollectionDate().getTime()));
                stmt.setString(2, obj.getMaterial());
                stmt.setDouble(3, obj.getTemperature());
                stmt.setLong(4, obj.getVolume());
                stmt.setString(5, obj.getVolumeUnit());
                stmt.setLong(6, obj.getHabitatId());

                stmt.setLong(7, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Sample for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public Result<TaskI> delete(long id) throws IOException {

        Result<AutoCloseableIterator<DNAExtract>> res = getController().getDNAExtractDAO().bySample(id);
        if (res.isError()) {
            return Result.error(res.getError());
        }

        List<TaskI> subtasks = new ArrayList<>();
        try ( AutoCloseableIterator<DNAExtract> iter = res.getValue();) {
            while (iter.hasNext()) {
                DNAExtract d = iter.next();
                Result<TaskI> delRun = getController().getDNAExtractDAO().delete(d.getId());
                if (delRun.isError()) {
                    return Result.error(delRun.getError());
                }
                subtasks.add(delRun.getValue());
            }
        }
        TaskI t = new DeleteSample(id, getController().getDataSource(),
                getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
        return Result.ok(t);
    }

//    public void delete(long id) throws MGXException {
//        try (Connection conn = getConnection()) {
//            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM sample WHERE id=?")) {
//                stmt.setLong(1, id);
//                int numRows = stmt.executeUpdate();
//                if (numRows != 1) {
//                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
//                }
//            }
//        } catch (SQLException ex) {
//            getController().log(ex);
//            throw new MGXException(ex);
//        }
//    }
    private final static String BY_ID = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit, s.habitat_id "
            + "FROM sample s WHERE s.id=?";

    @Override
    public Result<Sample> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type Sample for ID " + id + ".");
                    }
                    Sample s = new Sample();
                    s.setId(rs.getLong(1));
                    s.setCollectionDate(new java.util.Date(rs.getDate(2).getTime()));
                    s.setMaterial(rs.getString(3));
                    s.setTemperature(rs.getDouble(4));
                    s.setVolume(rs.getInt(5));
                    s.setVolumeUnit(rs.getString(6));
                    s.setHabitatId(rs.getLong(7));
                    return Result.ok(s);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String FETCHALL = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit, s.habitat_id "
            + "FROM sample s";

    public Result<AutoCloseableIterator<Sample>> getAll() {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Sample> dbIterator = new DBIterator<Sample>(rs, stmt, conn) {
                @Override
                public Sample convert(ResultSet rs) throws SQLException {
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
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String SQL_BY_HABITAT = "SELECT s.id, s.collectiondate, s.material, s.temperature, s.volume, s.volume_unit "
            + "FROM sample s WHERE s.habitat_id=?";

    public Result<AutoCloseableIterator<Sample>> byHabitat(final long habitat_id) {
        if (habitat_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_BY_HABITAT);
            stmt.setLong(1, habitat_id);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Sample> dbIterator = new DBIterator<Sample>(rs, stmt, conn) {
                @Override
                public Sample convert(ResultSet rs) throws SQLException {
                    Sample s = new Sample();
                    s.setHabitatId(habitat_id);
                    s.setId(rs.getLong(1));
                    s.setCollectionDate(new java.util.Date(rs.getDate(2).getTime()));
                    s.setMaterial(rs.getString(3));
                    s.setTemperature(rs.getDouble(4));
                    s.setVolume(rs.getInt(5));
                    s.setVolumeUnit(rs.getString(6));
                    return s;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

}
