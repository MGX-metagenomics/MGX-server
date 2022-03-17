package de.cebitec.mgx.upload;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTO;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTOList;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.ReferenceRegion;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.io.BufferedWriter;
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

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ReferenceUploadReceiver implements UploadReceiverI<ReferenceRegionDTOList> {

    private final Reference reference;
    private final long referenceId;
    private final String refFile;
    private final String refName;
    //
    private final String projectName;
    private GPMSManagedDataSourceI dataSource;
    //
    private long lastAccessed;
    private File fasta = null;
    private BufferedWriter writer = null;
    private int dnaSize = 0;
    private List<ReferenceRegion> regions;

    public ReferenceUploadReceiver(Reference reference, String projectName, GPMSManagedDataSourceI dataSource) {
        this.reference = reference;
        this.refFile = reference.getFile();
        this.referenceId = reference.getId();
        this.refName = reference.getName();
        this.projectName = projectName;
        this.dataSource = dataSource;

        dataSource.subscribe(this);
        lastAccessed = System.currentTimeMillis();
    }

    public void addSequenceData(String dna) throws IOException {
        if (fasta == null) {
            fasta = new File(refFile);
            writer = new BufferedWriter(new FileWriter(fasta));
            writer.append(">");
            writer.append(refName);
            writer.newLine();
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
        if (dataSource != null) {
            dataSource.close(this);
            dataSource = null;
        }
    }

    @Override
    public void close() throws MGXException {
        if (dnaSize != reference.getLength()) {
            throw new MGXException("Sequence length mismatch");
        }
        if (writer != null) {
            try {
                writer.newLine();
                writer.close();
                writer = null;
            } catch (IOException ex) {
                throw new MGXException(ex.getMessage());
            }
        }
        //reference.setRegions(regions);
        reference.setFile(fasta.getAbsolutePath());

        try (Connection conn = dataSource.getConnection(this)) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE reference SET ref_filepath=? WHERE id=?")) {
                stmt.setString(1, refFile);
                stmt.setLong(2, referenceId);
                stmt.execute();
            }
        } catch (SQLException ex) {
            cancel();
            throw new MGXException(ex.getMessage());
        }

        if (regions != null && !regions.isEmpty()) {
            try (Connection conn = dataSource.getConnection(this)) {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO region (name, description, type, reg_start, reg_stop, ref_id) VALUES (?,?,?,?,?,?)")) {
                    for (ReferenceRegion r : regions) {
                        stmt.setString(1, r.getName());
                        stmt.setString(2, r.getDescription());
                        stmt.setString(3, r.getType().toString());
                        stmt.setInt(4, r.getStart());
                        stmt.setInt(5, r.getStop());
                        stmt.setLong(6, referenceId);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReferenceUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
                while (ex.getNextException() != null) {
                    ex = ex.getNextException();
                    Logger.getLogger(ReferenceUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
                }
                cancel();
                throw new MGXException(ex.getMessage());
            }
        }

        if (dataSource != null) {
            dataSource.close(this);
            dataSource = null;
        }
    }

    @Override
    public void add(ReferenceRegionDTOList data) throws MGXException {
        if (regions == null) {
            regions = new LinkedList<>();
        }
        for (ReferenceRegionDTO dto : data.getRegionList()) {
            ReferenceRegion r = new ReferenceRegion();
            r.setName(dto.getName());
            r.setDescription(dto.getDescription());
            r.setType(RegionType.values()[dto.getType().ordinal()]);
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
