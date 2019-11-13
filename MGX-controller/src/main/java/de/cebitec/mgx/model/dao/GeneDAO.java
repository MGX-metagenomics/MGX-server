package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.model.db.GeneAnnotation;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class GeneDAO extends DAO<Gene> {

    public GeneDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Gene.class;
    }

    private final static String CREATE = "INSERT INTO gene (start, stop, coverage, contig_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Gene obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setInt(1, obj.getStart());
                stmt.setInt(2, obj.getStop());
                stmt.setInt(3, obj.getCoverage());
                stmt.setLong(4, obj.getContigId());

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

    private final static String UPDATE = "UPDATE gene SET start=?, stop=?, coverage=?, contig_id=? WHERE id=?";

    public void update(Gene obj) throws MGXException {
        if (obj.getId() == Contig.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setInt(1, obj.getStart());
                stmt.setInt(2, obj.getStop());
                stmt.setInt(3, obj.getCoverage());
                stmt.setLong(4, obj.getContigId());
                stmt.setLong(5, obj.getId());

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
    private static final String FETCHALL = "SELECT id, start, stop, coverage, contig_id FROM gene";
    private static final String BY_ID = "SELECT id, start, stop, coverage, contig_id FROM gene WHERE id=?";

    @Override
    public Gene getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Gene ret = new Gene();
                        ret.setId(rs.getLong(1));
                        ret.setStart(rs.getInt(2));
                        ret.setStop(rs.getInt(3));
                        ret.setCoverage(rs.getInt(4));
                        ret.setContigId(rs.getLong(5));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
    }

    public AutoCloseableIterator<Gene> getAll() throws MGXException {

        List<Gene> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {

                        Gene ret = new Gene();
                        ret.setId(rs.getLong(1));
                        ret.setStart(rs.getInt(2));
                        ret.setStop(rs.getInt(3));
                        ret.setCoverage(rs.getInt(4));
                        ret.setContigId(rs.getLong(5));

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

    private static final String BY_CONTIG = "SELECT g.id, g.start, g.stop, g.coverage FROM contig c "
            + "LEFT JOIN gene g ON (g.contig_id=c.id) WHERE c.id=?";

    public AutoCloseableIterator<Gene> byContig(long contig_id) throws MGXException {

        List<Gene> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_CONTIG)) {
                stmt.setLong(1, contig_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Contig for ID " + contig_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Gene ret = new Gene();
                            ret.setId(rs.getLong(1));
                            ret.setStart(rs.getInt(2));
                            ret.setStop(rs.getInt(3));
                            ret.setCoverage(rs.getInt(4));
                            ret.setContigId(contig_id);

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

    private static final String BY_CONTIGS = "SELECT g.id, g.start, g.stop, g.coverage, g.contig_id FROM gene g "
            + "WHERE g.contig_id IN (";

    public AutoCloseableIterator<Gene> byContigs(Collection<Long> ids) throws MGXException {

        if (ids == null || ids.isEmpty()) {
            throw new MGXException("Null/empty ID list.");
        }

        String sql = BY_CONTIGS + toSQLTemplateString(ids.size()) + ")";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            int idx = 1;
            for (Long id : ids) {
                stmt.setLong(idx++, id);
            }

            ResultSet rs = stmt.executeQuery();
            return new DBIterator<Gene>(rs, stmt, conn) {
                @Override
                public Gene convert(ResultSet rs) throws SQLException {
                    Gene ret = new Gene();
                    ret.setId(rs.getLong(1));
                    ret.setStart(rs.getInt(2));
                    ret.setStop(rs.getInt(3));
                    ret.setCoverage(rs.getInt(4));
                    ret.setContigId(rs.getLong(5));
                    return ret;
                }
            };

        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public void createCoverage(long geneId, long runId, int coverage) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO gene_coverage (run_id, gene_id, coverage) VALUES (?,?,?)")) {
                stmt.setLong(1, runId);
                stmt.setLong(2, geneId);
                stmt.setInt(3, coverage);

                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("Could not store coverage information.");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public TaskI delete(long id) throws MGXException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public AutoCloseableIterator<Gene> byBin(Long id) throws MGXException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private final static String SQL_BULK_GENE
            = "INSERT INTO gene_observation (start, stop, attr_id, gene_id) VALUES (?, ?, ?, ?)";

    public void createAnnotations(List<GeneAnnotation> annots) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BULK_GENE)) {
                for (GeneAnnotation obs : annots) {
                    stmt.setInt(1, obs.getStart());
                    stmt.setInt(2, obs.getStop());
                    stmt.setLong(3, obs.getAttributeId());
                    stmt.setLong(4, obs.getGeneId());
                    stmt.addBatch();
                }
                int[] status = stmt.executeBatch();

                if (status == null || status.length != annots.size()) {
                    throw new MGXException("Database batch update failed. Expected " + annots.size()
                            + ", got " + (status == null ? "null" : String.valueOf(status.length)));
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            SQLException sqle = ex;
            while (sqle.getNextException() != null) {
                sqle = sqle.getNextException();
                getController().log(sqle);
            }
            throw new MGXException(ex.getMessage());
        }
    }
}
