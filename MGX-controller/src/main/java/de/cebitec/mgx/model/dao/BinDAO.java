package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.workers.DeleteBin;
import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class BinDAO extends DAO<Bin> {
    
    public BinDAO(MGXController ctx) {
        super(ctx);
    }
    
    @Override
    Class<Bin> getType() {
        return Bin.class;
    }
    
    private final static String CREATE = "INSERT INTO bin (name, completeness, contamination, taxonomy, n50, assembly_id) "
            + "VALUES (?,?,?,?,?,?) RETURNING id";
    
    @Override
    public long create(Bin obj) throws MGXException {

        //
        // for the unclassified bin, reset taxonomic assignment
        //
        if (obj.getName().toLowerCase().equals("unclassified") || obj.getName().toLowerCase().equals("unbinned")) {
            obj.setTaxonomy("root");
        }
        
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setFloat(2, obj.getCompleteness());
                stmt.setFloat(3, obj.getContamination());
                stmt.setString(4, obj.getTaxonomy());
                stmt.setLong(5, obj.getN50());
                stmt.setLong(6, obj.getAssemblyId());
                
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
    
    private final static String UPDATE = "UPDATE bin SET name=?, completeness=?, contamination=?, taxonomy=?, n50=?, predicted_cds=?, total_bp=?, assembly_id=? WHERE id=?";
    
    public void update(Bin obj) throws MGXException {
        if (obj.getId() == Bin.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type Bin without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setFloat(2, obj.getCompleteness());
                stmt.setFloat(3, obj.getContamination());
                stmt.setString(4, obj.getTaxonomy());
                stmt.setLong(5, obj.getN50());
                stmt.setInt(6, obj.getPredictedCDS());
                stmt.setLong(7, obj.getTotalBp());
                stmt.setLong(8, obj.getAssemblyId());
                
                stmt.setLong(9, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Bin for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }
    
    private static final String BY_ID = "SELECT id, name, completeness, contamination, taxonomy, n50, predicted_cds, total_bp, assembly_id FROM bin WHERE id=?";
    
    @Override
    public Result<Bin> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Bin ret = new Bin();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setCompleteness(rs.getFloat(3));
                        ret.setContamination(rs.getFloat(4));
                        ret.setTaxonomy(rs.getString(5));
                        ret.setN50(rs.getInt(6));
                        ret.setPredictedCDS(rs.getInt(7));
                        ret.setTotalBp(rs.getLong(8));
                        ret.setAssemblyId(rs.getLong(9));
                        return Result.ok(ret);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        
        return Result.error("No object of type Bin for ID " + id + ".");
    }
    
    private static final String FETCHALL = "SELECT id, name, completeness, contamination, taxonomy, n50, predicted_cds, total_bp, assembly_id FROM bin";
    
    public Result<AutoCloseableIterator<Bin>> getAll() {
        
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();
            
            DBIterator<Bin> dbIterator = new DBIterator<Bin>(rs, stmt, conn) {
                @Override
                public Bin convert(ResultSet rs) throws SQLException {
                    Bin ret = new Bin();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setCompleteness(rs.getFloat(3));
                    ret.setContamination(rs.getFloat(4));
                    ret.setTaxonomy(rs.getString(5));
                    ret.setN50(rs.getInt(6));
                    ret.setPredictedCDS(rs.getInt(7));
                    ret.setTotalBp(rs.getLong(8));
                    ret.setAssemblyId(rs.getLong(9));
                    return ret;
                }
            };
            return Result.ok(dbIterator);
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }
    
    private static final String BY_ASM = "SELECT b.id, b.name, b.completeness, b.contamination, b.taxonomy, b.n50, b.num_contigs, b.total_bp, b.predicted_cds FROM bin b WHERE b.assembly_id=?";
    
    public Result<AutoCloseableIterator<Bin>> byAssembly(final long asm_id) {
        
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_ASM);
            stmt.setLong(1, asm_id);
            ResultSet rs = stmt.executeQuery();
            
            DBIterator<Bin> dbIterator = new DBIterator<Bin>(rs, stmt, conn) {
                @Override
                public Bin convert(ResultSet rs) throws SQLException {
                    Bin ret = new Bin();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setCompleteness(rs.getFloat(3));
                    ret.setContamination(rs.getFloat(4));
                    ret.setTaxonomy(rs.getString(5));
                    ret.setN50(rs.getInt(6));
                    ret.setNumContigs(rs.getInt(7));
                    ret.setTotalBp(rs.getLong(8));
                    ret.setPredictedCDS(rs.getInt(9));
                    ret.setAssemblyId(asm_id);
                    return ret;
                }
            };
            return Result.ok(dbIterator);
            
        } catch (SQLException ex) {
            return Result.error(ex.getMessage());
        }
    }
    
    public Result<TaskI> delete(long bin_id) {
        Result<Bin> res = getById(bin_id);
        if (res.isError()) {
            return Result.error(res.getError());
        }
        Bin bin = res.getValue();
        
        try {
            File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(bin.getAssemblyId()));
            File binFasta = new File(assemblyDir, String.valueOf(bin_id) + ".fna");
            binFasta.delete();
            File binFastaIdx = new File(assemblyDir, String.valueOf(bin_id) + ".fna.fai");
            binFastaIdx.delete();
        } catch (IOException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        
        TaskI t = new DeleteBin(getController().getDataSource(), bin_id, getController().getProjectName(), new TaskI[]{});
        return Result.ok(t);
    }
    
    private static final String BINS_BY_ASM_TMPL = "WITH temp as ( "
            + "SELECT b.id, b.name, b.completeness, b.contamination, b.taxonomy, b.n50, count(c.id), sum(c.length_bp) FROM assembly a "
            + "LEFT JOIN bin b ON (a.id=b.assembly_id) "
            + "LEFT JOIN contig c ON (b.id=c.bin_id) "
            + "WHERE a.id=? "
            + "GROUP BY b.id "
            + "ORDER BY b.id ASC "
            + ")"
            + "SELECT t.id, t.name, t.completeness, t.contamination, t.taxonomy, t.n50, t.count, t.sum, count(g.id) FROM temp t "
            + "LEFT JOIN contig c ON (t.id=c.bin_id) "
            + "LEFT JOIN gene g ON (c.id=g.contig_id) "
            + "GROUP BY t.id, t.name, t.completeness, t.contamination, t.taxonomy, t.n50, t.count, t.sum "
            + "ORDER BY t.id ASC";
    
    public void updateDerivedFields(long asm_id) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BINS_BY_ASM_TMPL)) {
                stmt.setLong(1, asm_id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Bin ret = new Bin();
                        ret.setId(rs.getLong(1));
                        //ret.setName(rs.getString(2));
                        //ret.setCompleteness(rs.getFloat(3));
                        //ret.setContamination(rs.getFloat(4));
                        //ret.setTaxonomy(rs.getString(5));
                        //ret.setN50(rs.getInt(6));
                        ret.setNumContigs(rs.getInt(7));
                        ret.setTotalBp(rs.getLong(8));
                        ret.setPredictedCDS(rs.getInt(9));
                        //ret.setAssemblyId(asm_id);

                        try ( PreparedStatement stmt2 = conn.prepareStatement("UPDATE bin SET predicted_cds=?, num_contigs=?, total_bp=? WHERE id=?")) {
                            stmt2.setInt(1, ret.getPredictedCDS());
                            stmt2.setInt(2, ret.getNumContigs());
                            stmt2.setLong(3, ret.getTotalBp());
                            stmt2.setLong(4, ret.getId());
                            stmt2.executeUpdate();
                        }
                        
                        indexFASTA(asm_id, ret.getId());
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(BinDAO.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
    }
    
    public void indexFASTA(File binFasta) throws MGXException {
        File dir = binFasta.getParentFile();
        if (!dir.exists() && dir.canWrite()) {
            Logger.getLogger(BinDAO.class.getName()).log(Level.SEVERE, "Cannot write to directory {0}", dir.getAbsolutePath());
            throw new MGXException("Cannot write to directory " + dir.getAbsolutePath());
        }
        try {
            getController().log("Creating FASTA index for " + binFasta.getAbsolutePath());
            FastaSequenceIndexCreator.create(binFasta.toPath(), true);
        } catch (IOException ex) {
            Logger.getLogger(BinDAO.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
        File indexFile = new File(binFasta.getAbsolutePath() + ".fai");
        
        if (!indexFile.exists()) {
            Logger.getLogger(BinDAO.class.getName()).log(Level.SEVERE, "Index creation failed for {0}", binFasta.getAbsolutePath());
            throw new MGXException("Index creation failed.");
        }
    }
    
    private void indexFASTA(long asm_id, long bin_id) throws MGXException {
        try {
            File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(asm_id));
            File binFasta = new File(assemblyDir, String.valueOf(bin_id) + ".fna");
            if (!binFasta.exists() || !binFasta.canRead()) {
                throw new MGXException("Unable to access FASTA file for bin " + bin_id + ".");
            }
            FastaSequenceIndexCreator.buildFromFasta(Paths.get(binFasta.getAbsolutePath()));
        } catch (IOException ex) {
            Logger.getLogger(BinDAO.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
    }
}
