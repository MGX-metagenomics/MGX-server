package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
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
    Class<Contig> getType() {
        return Contig.class;
    }

    private final static String CREATE = "INSERT INTO contig (name, length_bp, gc, coverage, bin_id) "
            + "VALUES (?,?,?,?,?) RETURNING id";

    @Override
    public long create(Contig obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getLength());
                stmt.setFloat(3, obj.getGC());
                stmt.setInt(4, obj.getCoverage());
                stmt.setLong(5, obj.getBinId());

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

    private final static String UPDATE = "UPDATE contig SET name=?, length_bp=?, gc=?, coverage=?, bin_id=? WHERE id=?";

    public void update(Contig obj) throws MGXException {
        if (obj.getId() == Contig.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type Contig without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getLength());
                stmt.setFloat(3, obj.getGC());
                stmt.setInt(4, obj.getCoverage());
                stmt.setLong(5, obj.getBinId());

                stmt.setLong(6, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Contig for ID " + obj.getId() + ".");
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
    private static final String BY_ID = "SELECT c.id, c.name, c.length_bp, c.gc, c.coverage, c.bin_id, COUNT(g.contig_id) "
            + "FROM contig c "
            + "LEFT JOIN gene g ON (c.id=g.contig_id) "
            + "WHERE id=? GROUP BY c.id";

    @Override
    public Result<Contig> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Contig ret = new Contig();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setLength(rs.getInt(3));
                        ret.setGC(rs.getFloat(4));
                        ret.setCoverage(rs.getInt(5));
                        ret.setBinId(rs.getLong(6));
                        ret.setNumberSubregions(rs.getInt(7));
                        return Result.ok(ret);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        return Result.error("No object of type Contig for ID " + id + ".");
    }

    private static final String BY_IDS = "SELECT c.id, c.name, c.length_bp, c.gc, c.coverage, c.bin_id FROM contig c "
            + "LEFT JOIN gene g ON (c.id=g.contig_id) "
            + "WHERE id IN (";

    public Result<AutoCloseableIterator<Contig>> getByIds(long... ids) {
        if (ids == null || ids.length == 0) {
            return Result.error("Null/empty ID list.");
        }

        String query = BY_IDS + toSQLTemplateString(ids.length) + ") GROUP BY c.id";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            int idx = 1;
            for (long id : ids) {
                if (id <= 0) {
                    return Result.error("No/Invalid ID supplied.");
                }
                stmt.setLong(idx++, id);
            }
            ResultSet rs = stmt.executeQuery();

            DBIterator<Contig> dbIterator = new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setBinId(rs.getLong(6));
                    ret.setNumberSubregions(rs.getInt(7));
                    return ret;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private static final String FETCHALL = "SELECT c.id, c.name, c.length_bp, c.gc, c.coverage, c.bin_id, COUNT(g.contig_id) "
            + "FROM contig c "
            + "LEFT JOIN gene g ON (c.id=g.contig_id) "
            + "GROUP BY c.id ORDER BY c.length_bp DESC";

    public Result<AutoCloseableIterator<Contig>> getAll() {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Contig> dbIterator = new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setBinId(rs.getLong(6));
                    ret.setNumberSubregions(rs.getInt(7));
                    return ret;
                }

            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private static final String BY_BIN = "SELECT c.id, c.name, c.length_bp, c.gc, c.coverage, COUNT(g.contig_id) "
            + "FROM contig c "
            + "LEFT JOIN gene g ON (c.id=g.contig_id) "
            + "WHERE c.bin_id=? GROUP BY c.id ORDER BY c.length_bp DESC";

    public Result<AutoCloseableIterator<Contig>> byBin(final long bin_id) {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_BIN);
            stmt.setLong(1, bin_id);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Contig> dbIterator = new DBIterator<Contig>(rs, stmt, conn) {
                @Override
                public Contig convert(ResultSet rs) throws SQLException {
                    Contig ret = new Contig();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setLength(rs.getInt(3));
                    ret.setGC(rs.getFloat(4));
                    ret.setCoverage(rs.getInt(5));
                    ret.setNumberSubregions(rs.getInt(6));
                    ret.setBinId(bin_id);
                    return ret;
                }
            };
            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private final static String SQL_FETCH = "SELECT c.name, c.bin_id, b.assembly_id "
            + "FROM contig c "
            + "LEFT JOIN bin b ON (c.bin_id=b.id) "
            + "WHERE c.id=?";

    public Result<Sequence> getDNASequence(long contig_id) {

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_FETCH)) {
                stmt.setLong(1, contig_id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type Contig for ID " + contig_id + ".");
                    }

                    String contig_name = rs.getString(1);
                    long bin_id = rs.getLong(2);
                    long assembly_id = rs.getLong(3);
                    String contigSeq;

                    File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(assembly_id));
                    File binFasta = new File(assemblyDir, String.valueOf(bin_id) + ".fna");

                    File fastaIdx = new File(assemblyDir, String.valueOf(bin_id) + ".fna.fai");
                    if (!fastaIdx.exists()) {
                        // create index
                        getController().getBinDAO().indexFASTA(binFasta);
                    }
                    try ( IndexedFastaSequenceFile ifsf = new IndexedFastaSequenceFile(binFasta)) {
                        ReferenceSequence seq = ifsf.getSequence(contig_name);

                        if (seq == null || seq.length() == 0) {
                            return Result.error("No sequence found for contig " + contig_name);
                        }
                        contigSeq = new String(seq.getBases());
                    }

                    Sequence ret = new Sequence();
                    ret.setId(contig_id);
                    ret.setName(contig_name);
                    ret.setSequence(contigSeq);
                    ret.setLength(contigSeq.length());

                    return Result.ok(ret);
                }
            }
        } catch (SQLException | IOException | MGXException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<TaskI> delete(long id) {
        // FIXME
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
