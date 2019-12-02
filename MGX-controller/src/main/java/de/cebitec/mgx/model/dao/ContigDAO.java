package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class ContigDAO extends DAO<Contig> {

    public ContigDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Contig.class;
    }

    private final static String CREATE = "INSERT INTO contig (name, length_bp, gc, coverage, bin_id) "
            + "VALUES (?,?,?,?,?) RETURNING id";

    @Override
    public long create(Contig obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getLength());
                stmt.setFloat(3, obj.getGC());
                stmt.setInt(4, obj.getCoverage());
                stmt.setLong(5, obj.getBinId());

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

    private final static String UPDATE = "UPDATE contig SET name=?, length_bp=?, gc=?, coverage=?, bin_id=? WHERE id=?";

    public void update(Contig obj) throws MGXException {
        if (obj.getId() == Contig.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getLength());
                stmt.setFloat(3, obj.getGC());
                stmt.setInt(4, obj.getCoverage());
                stmt.setLong(5, obj.getBinId());

                stmt.setLong(6, obj.getId());
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
    private static final String FETCHALL = "SELECT id, name, length_bp, gc, coverage, bin_id FROM contig";
    private static final String BY_ID = "SELECT id, name, length_bp, gc, coverage, bin_id FROM contig WHERE id=?";
    private static final String BY_IDS = "SELECT id, name, length_bp, gc, coverage, bin_id FROM contig WHERE id IN (";

    @Override
    public Contig getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Contig ret = new Contig();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setLength(rs.getInt(3));
                        ret.setGC(rs.getFloat(4));
                        ret.setCoverage(rs.getInt(5));
                        ret.setBinId(rs.getLong(6));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
    }

    public AutoCloseableIterator<Contig> getByIds(long... ids) throws MGXException {
        if (ids == null || ids.length == 0) {
            throw new MGXException("Null/empty ID list.");
        }

        String query = BY_IDS + toSQLTemplateString(ids.length) + ")";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            int idx = 1;
            for (long id : ids) {
                if (id <= 0) {
                    throw new MGXException("No/Invalid ID supplied.");
                }
                stmt.setLong(idx++, id);
            }
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setBinId(rs.getLong(6));
                    return ret;
                }
            };

        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

    }

    public AutoCloseableIterator<Contig> getAll() throws MGXException {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setBinId(rs.getLong(6));
                    return ret;
                }

            };

        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    private static final String BY_BIN = "SELECT c.id, c.name, c.length_bp, c.gc, c.coverage FROM contig c "
            + "WHERE c.bin_id=?";

    //
    // FIXME get num cds
    //
    public AutoCloseableIterator<Contig> byBin(final long bin_id) throws MGXException {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_BIN);
            stmt.setLong(1, bin_id);
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setBinId(bin_id);
                    return ret;
                }
            };
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public Sequence getDNASequence(long contig_id) throws MGXException {
        Contig contig = getById(contig_id);
        Bin bin = getController().getBinDAO().getById(contig.getBinId());
        String contigSeq;
        try {
            File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(bin.getAssemblyId()));
            File binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
            try (IndexedFastaSequenceFile ifsf = new IndexedFastaSequenceFile(binFasta)) {
                ReferenceSequence seq = ifsf.getSequence(contig.getName());

                if (seq == null || seq.length() == 0) {
                    throw new MGXException("No sequence found for contig " + contig.getName());
                }
                contigSeq = new String(seq.getBases());
            }

        } catch (IOException ex) {
            throw new MGXException(ex);
        }

        Sequence ret = new Sequence();
        ret.setId(contig_id);
        ret.setName(contig.getName());
        ret.setSequence(contigSeq);

        return ret;
    }

    public TaskI delete(long id) throws MGXException {
        // FIXME
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
