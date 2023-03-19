package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dnautils.DNAUtils;
import de.cebitec.mgx.model.db.AssembledRegion;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.GeneAnnotation;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.misc.BinSearchResult;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import java.io.File;
import java.io.IOException;
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
public class AssembledRegionDAO extends DAO<AssembledRegion> {

    public AssembledRegionDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<AssembledRegion> getType() {
        return AssembledRegion.class;
    }

    private final static String CREATE = "INSERT INTO gene (start, stop, coverage, type, contig_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(AssembledRegion obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setInt(1, obj.getStart());
                stmt.setInt(2, obj.getStop());
                stmt.setInt(3, obj.getCoverage());
                stmt.setString(4, obj.getType().toString());
                stmt.setLong(5, obj.getContigId());

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

    private final static String UPDATE = "UPDATE gene SET start=?, stop=?, coverage=?, type=?, contig_id=? WHERE id=?";

    public void update(AssembledRegion obj) throws MGXException {
        if (obj.getId() == Contig.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type AssembledRegion without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setInt(1, obj.getStart());
                stmt.setInt(2, obj.getStop());
                stmt.setInt(3, obj.getCoverage());
                stmt.setString(4, obj.getType().toString());
                stmt.setLong(5, obj.getContigId());
                stmt.setLong(6, obj.getId());

                stmt.setLong(6, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type AssembledRegion for ID " + obj.getId() + ".");
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
    private static final String FETCHALL = "SELECT id, start, stop, coverage, type, contig_id FROM gene";
    private static final String BY_ID = "SELECT id, start, stop, coverage, type, contig_id FROM gene WHERE id=?";

    @Override
    public AssembledRegion getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        AssembledRegion ret = new AssembledRegion();
                        ret.setId(rs.getLong(1));
                        ret.setStart(rs.getInt(2));
                        ret.setStop(rs.getInt(3));
                        ret.setCoverage(rs.getInt(4));

                        String type = rs.getString(5);
                        switch (type) {
                            case "CDS":
                                ret.setType(RegionType.CDS);
                                break;
                            case "tRNA":
                                ret.setType(RegionType.TRNA);
                                break;
                            case "rRNA":
                                ret.setType(RegionType.RRNA);
                                break;
                            case "tmRNA":
                                ret.setType(RegionType.TMRNA);
                                break;
                            case "ncRNA":
                                ret.setType(RegionType.NCRNA);
                                break;
                            default:
                                getController().log("Unhandled region type " + type + "for ID " + id);
                                ret.setType(RegionType.MISC);

                        }

                        ret.setContigId(rs.getLong(6));
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type AssembledRegion for ID " + id + ".");
    }

    public AutoCloseableIterator<AssembledRegion> getAll() throws MGXException {

        List<AssembledRegion> l = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try ( ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {

                        AssembledRegion ret = new AssembledRegion();
                        ret.setId(rs.getLong(1));
                        ret.setStart(rs.getInt(2));
                        ret.setStop(rs.getInt(3));
                        ret.setCoverage(rs.getInt(4));

                        String type = rs.getString(5);
                        switch (type) {
                            case "CDS":
                                ret.setType(RegionType.CDS);
                                break;
                            case "tRNA":
                                ret.setType(RegionType.TRNA);
                                break;
                            case "rRNA":
                                ret.setType(RegionType.RRNA);
                                break;
                            case "tmRNA":
                                ret.setType(RegionType.TMRNA);
                                break;
                            case "ncRNA":
                                ret.setType(RegionType.NCRNA);
                                break;
                            default:
                                getController().log("Unhandled region type " + type + "for ID " + ret.getId());
                                ret.setType(RegionType.MISC);

                        }

                        ret.setContigId(rs.getLong(6));

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

    private static final String BY_CONTIG = "SELECT g.id, g.start, g.stop, g.coverage, g.type FROM contig c "
            + "LEFT JOIN gene g ON (g.contig_id=c.id) WHERE c.id=?";

    public AutoCloseableIterator<AssembledRegion> byContig(long contig_id) throws MGXException {

        List<AssembledRegion> l = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_CONTIG)) {
                stmt.setLong(1, contig_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Contig for ID " + contig_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            AssembledRegion ret = new AssembledRegion();
                            ret.setId(rs.getLong(1));
                            ret.setStart(rs.getInt(2));
                            ret.setStop(rs.getInt(3));
                            ret.setCoverage(rs.getInt(4));

                            String type = rs.getString(5);
                            switch (type) {
                                case "CDS":
                                    ret.setType(RegionType.CDS);
                                    break;
                                case "tRNA":
                                    ret.setType(RegionType.TRNA);
                                    break;
                                case "rRNA":
                                    ret.setType(RegionType.RRNA);
                                    break;
                                case "tmRNA":
                                    ret.setType(RegionType.TMRNA);
                                    break;
                                case "ncRNA":
                                    ret.setType(RegionType.NCRNA);
                                    break;
                                default:
                                    getController().log("Unhandled region type " + type + "for ID " + ret.getId());
                                    ret.setType(RegionType.MISC);

                            }

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

    private static final String BY_CONTIGS = "SELECT g.id, g.start, g.stop, g.coverage, g.type, g.contig_id FROM gene g "
            + "WHERE g.contig_id IN (";

    public AutoCloseableIterator<AssembledRegion> byContigs(Collection<Long> ids) throws MGXException {

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
            return new DBIterator<AssembledRegion>(rs, stmt, conn) {
                @Override
                public AssembledRegion convert(ResultSet rs) throws SQLException {
                    AssembledRegion ret = new AssembledRegion();
                    ret.setId(rs.getLong(1));
                    ret.setStart(rs.getInt(2));
                    ret.setStop(rs.getInt(3));
                    ret.setCoverage(rs.getInt(4));

                    String type = rs.getString(5);
                    switch (type) {
                        case "CDS":
                            ret.setType(RegionType.CDS);
                            break;
                        case "tRNA":
                            ret.setType(RegionType.TRNA);
                            break;
                        case "rRNA":
                            ret.setType(RegionType.RRNA);
                            break;
                        case "tmRNA":
                            ret.setType(RegionType.TMRNA);
                            break;
                        case "ncRNA":
                            ret.setType(RegionType.NCRNA);
                            break;
                        default:
                            getController().log("Unhandled region type " + type + "for ID " + ret.getId());
                            ret.setType(RegionType.MISC);

                    }
                    ret.setContigId(rs.getLong(6));
                    return ret;
                }
            };

        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public void createCoverage(long geneId, long runId, int coverage) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("INSERT INTO gene_coverage (run_id, gene_id, coverage, type) VALUES (?,?,?)")) {
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

    public AutoCloseableIterator<AssembledRegion> byBin(Long id) throws MGXException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private final static String SQL_SEARCH
            = "SELECT c.id, c.name, g.id, a.value, at.name"
            + " FROM contig c"
            + " LEFT JOIN gene g ON c.id=g.contig_id"
            + " LEFT JOIN gene_observation go ON g.id=go.gene_id"
            + " LEFT JOIN attribute a ON a.id=go.attr_id"
            + " LEFT JOIN attributetype at ON at.id=a.attrtype_id"
            + " WHERE c.bin_id=? AND UPPER(a.value) LIKE CONCAT('%', UPPER(?), '%')";

    public AutoCloseableIterator<BinSearchResult> search(Long bin_id, String term) throws MGXException {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_SEARCH);
            stmt.setLong(1, bin_id);
            stmt.setString(2, term);
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<BinSearchResult>(rs, stmt, conn) {
                @Override
                public BinSearchResult convert(ResultSet rs) throws SQLException {
                    BinSearchResult res = new BinSearchResult(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getLong(3),
                            rs.getString(4),
                            rs.getString(5)
                    );
                    return res;
                }

            };

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

    private final static String SQL_BULK_GENE
            = "INSERT INTO gene_observation (start, stop, attr_id, gene_id) VALUES (?, ?, ?, ?)";

    public void createAnnotations(List<GeneAnnotation> annots) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BULK_GENE)) {
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

    private final static String SQL_FETCH = "SELECT g.start, g.stop, c.name, c.bin_id, b.assembly_id "
            + "FROM gene g "
            + "LEFT JOIN contig c ON (g.contig_id=c.id) "
            + "LEFT JOIN bin b ON (c.bin_id=b.id) "
            + "WHERE g.id=?";

    public Sequence getDNASequence(long gene_id) throws MGXException {

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_FETCH)) {
                stmt.setLong(1, gene_id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type Gene for ID " + gene_id + ".");
                    }

                    int start = rs.getInt(1);
                    int stop = rs.getInt(2);
                    String contigName = rs.getString(3);
                    long bin_id = rs.getLong(4);
                    long assembly_id = rs.getLong(5);

                    File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(assembly_id));
                    File binFasta = new File(assemblyDir, String.valueOf(bin_id) + ".fna");
                    String geneSeq;
                    try ( IndexedFastaSequenceFile ifsf = new IndexedFastaSequenceFile(binFasta)) {
                        ReferenceSequence seq;
                        if (start < stop) {
                            // htsjdk uses 1-based positions
                            seq = ifsf.getSubsequenceAt(contigName, start + 1, stop + 1);
                            geneSeq = new String(seq.getBases());
                        } else {
                            seq = ifsf.getSubsequenceAt(contigName, stop + 1, start + 1);
                            geneSeq = DNAUtils.reverseComplement(new String(seq.getBases()));
                        }
                        if (seq == null || seq.length() == 0) {
                            throw new MGXException("No sequence found for contig " + contigName);
                        }
                    }

                    Sequence ret = new Sequence();
                    ret.setId(gene_id);
                    ret.setName(contigName + "_" + String.valueOf(gene_id));
                    ret.setSequence(geneSeq);

                    return ret;

                }
            }
        } catch (SQLException | IOException ex) {
            throw new MGXException(ex);
        }
    }
}
