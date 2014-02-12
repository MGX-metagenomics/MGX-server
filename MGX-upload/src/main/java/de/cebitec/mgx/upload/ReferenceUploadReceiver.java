package de.cebitec.mgx.upload;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.RegionDTO;
import de.cebitec.mgx.dto.dto.RegionDTOList;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ReferenceUploadReceiver implements UploadReceiverI<RegionDTOList> {

    private final Reference reference;
    private final String projectName;
    private final Connection conn;
    //
    private long lastAccessed;
    private File fasta = null;
    private FileWriter writer = null;
    private int dnaSize = 0;
    private List<Region> regions;

    public ReferenceUploadReceiver(Reference reference, String projectName, Connection conn) {
        this.reference = reference;
        this.projectName = projectName;
        this.conn = conn;
        lastAccessed = System.currentTimeMillis();
    }

    public void addSequenceData(String dna) throws IOException {
        if (fasta == null) {
            fasta = new File(reference.getFile());
            writer = new FileWriter(fasta);
            writer.append(">lcl|" + reference.getId() + " " + reference.getName() + "\n");
        }
        writer.append(dna.toUpperCase());
        dnaSize += dna.length();
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        if (writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException ex) {
                Logger.getLogger(ReferenceUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (fasta != null && fasta.exists()) {
            fasta.delete();
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(ReferenceUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void close() throws MGXException {
        if (dnaSize != reference.getLength()) {
            throw new MGXException("Sequence length mismatch");
        }
        if (writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException ex) {
                throw new MGXException(ex.getMessage());
            }
        }
        reference.setRegions(regions);
        reference.setFile(fasta.getAbsolutePath());
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE reference SET ref_filepath=? WHERE id=?")) {
            stmt.setString(1, reference.getFile());
            stmt.setLong(2, reference.getId());
            stmt.execute();
        } catch (SQLException ex) {
            cancel();
            throw new MGXException(ex.getMessage());
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region (name, description, reg_start, reg_stop, ref_id) VALUES (?,?,?,?,?)")) {
            for (Region r : regions) {
                stmt.setString(1, r.getName());
                stmt.setString(2, r.getDescription());
                stmt.setInt(3, r.getStart());
                stmt.setInt(4, r.getStop());
                stmt.setLong(5, reference.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(ReferenceUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            cancel();
            throw new MGXException(ex.getMessage());
        }
    }

    @Override
    public void add(RegionDTOList data) throws MGXException {
        if (regions == null) {
            regions = new LinkedList<>();
        }
        for (RegionDTO dto : data.getRegionList()) {
            Region r = new Region();
            r.setName(dto.getName());
            r.setDescription(dto.getDescription());
            r.setStart(dto.getStart());
            r.setStop(dto.getStop());
            regions.add(r);
        }
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }
}
