package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteBin;
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
public class BinDAO extends DAO<Bin> {

    public BinDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Bin.class;
    }

    private final static String CREATE = "INSERT INTO bin (name, completeness, contamination, taxonomy, n50, assembly_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Bin obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setFloat(2, obj.getCompleteness());
                stmt.setFloat(3, obj.getContamination());
                stmt.setString(4, obj.getTaxonomy());
                stmt.setLong(5, obj.getN50());
                stmt.setLong(6, obj.getAssemblyId());

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

    private final static String UPDATE = "UPDATE bin SET name=?, completeness=?, contamination=?, taxonomy=?, n50=?, assembly_id=? WHERE id=?";

    public void update(Bin obj) throws MGXException {
        if (obj.getId() == Bin.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setFloat(2, obj.getCompleteness());
                stmt.setFloat(3, obj.getContamination());
                stmt.setString(4, obj.getTaxonomy());
                stmt.setLong(5, obj.getN50());
                stmt.setLong(6, obj.getAssemblyId());

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

//    public TaskI delete(long id) throws MGXException, IOException {
//        List<TaskI> subtasks = new ArrayList<>();
//        try (AutoCloseableIterator<Sample> iter = getController().getSampleDAO().byHabitat(id)) {
//            while (iter.hasNext()) {
//                Sample s = iter.next();
//                TaskI del = getController().getSampleDAO().delete(s.getId());
//                subtasks.add(del);
//            }
//        }
//        return new DeleteHabitat(getController().getDataSource(), id, getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
//    }
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
    private static final String FETCHALL = "SELECT id, name, completeness, contamination, taxonomy, n50, predicted_cds, assembly_id FROM bin";
    private static final String BY_ID = "SELECT id, name, completeness, contamination, taxonomy, n50, assembly_id FROM bin WHERE id=?";

    @Override
    public Bin getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Bin ret = new Bin();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setCompleteness(rs.getFloat(3));
                        ret.setContamination(rs.getFloat(4));
                        ret.setTaxonomy(rs.getString(5));
                        ret.setN50(rs.getInt(6));
                        ret.setPredictedCDS(0);
                        //ret.setPredictedCDS(rs.getInt(7));
                        ret.setAssemblyId(rs.getLong(7));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
    }

    public AutoCloseableIterator<Bin> getAll() throws MGXException {

        List<Bin> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        Bin ret = new Bin();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setCompleteness(rs.getFloat(3));
                        ret.setContamination(rs.getFloat(4));
                        ret.setTaxonomy(rs.getString(5));
                        ret.setN50(rs.getInt(6));
                        ret.setPredictedCDS(rs.getInt(7));
                        ret.setAssemblyId(rs.getLong(8));

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

    private static final String BY_ASM = "SELECT b.id, b.name, b.completeness, b.contamination, b.taxonomy, b.n50, b.predicted_cds FROM assembly a"
            + "LET JOIN bin b ON (a.id=b.assembly_id) WHERE a.id=?";
    
    //
    // FIXME add total_bp field
    //

    public AutoCloseableIterator<Bin> byAssembly(long asm_id) throws MGXException {

        List<Bin> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ASM)) {
                stmt.setLong(1, asm_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Assembly for ID " + asm_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Bin ret = new Bin();
                            ret.setId(rs.getLong(1));
                            ret.setName(rs.getString(2));
                            ret.setCompleteness(rs.getFloat(3));
                            ret.setContamination(rs.getFloat(4));
                            ret.setTaxonomy(rs.getString(5));
                            ret.setN50(rs.getInt(6));
                            ret.setPredictedCDS(rs.getInt(7));
                            ret.setAssemblyId(rs.getLong(8));

                            if (l == null) {
                                l = new ArrayList<>();
                            }
                            l.add(ret);
                        }
                    } while (rs.next());

                }

            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(l == null ? null : l.iterator());
    }

    public TaskI delete(long bin_id) throws MGXException {
        List<TaskI> subtasks = new ArrayList<>();
//        try (AutoCloseableIterator<Bin> iter = getController().getBinDAO().byAssembly(id)) {
//            while (iter.hasNext()) {
//                Bin s = iter.next();
//                TaskI del = getController().getBinDAO().delete(s.getId());
//                subtasks.add(del);
//            }
//        }
        return new DeleteBin(getController().getDataSource(), bin_id, getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
    }
}
