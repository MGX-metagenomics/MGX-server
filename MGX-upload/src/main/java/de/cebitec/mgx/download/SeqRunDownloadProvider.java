package de.cebitec.mgx.download;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqRunDownloadProvider implements DownloadProviderI<SequenceDTOList> {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    protected final String projectName;
    protected final long runId;
    protected final Connection conn;
    protected SeqReaderI<DNASequenceHolder> reader;
    protected long lastAccessed;
    protected int bulksize;

    public SeqRunDownloadProvider(Connection pConn, String projName, long run_id) throws MGXException {
        projectName = projName;
        runId = run_id;

        try {
            mgxconfig = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration");
            File file = getStorageFile(run_id);
            reader = SeqReaderFactory.getReader(file.getAbsolutePath());
            conn = pConn;
            conn.setClientInfo("ApplicationName", "MGX-SeqDownload (" + projName + ")");
        } catch (NamingException | MGXException | SeqStoreException | SQLClientInfoException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }

        bulksize = mgxconfig.getSQLBulkInsertSize();
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        try {
            reader.close();
            conn.setClientInfo("ApplicationName", "");
            conn.close();
        } catch (Exception ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() throws MGXException {
        try {
            reader.close();
            conn.setClientInfo("ApplicationName", "");
            conn.close();
        } catch (Exception ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SequenceDTOList fetch() throws MGXException {
        Builder listBuilder = SequenceDTOList.newBuilder();
        int count = 0;
        while (count <= bulksize && reader.hasMoreElements()) {
            DNASequenceI seq = reader.nextElement().getSequence();
            SequenceDTO dto = SequenceDTO.newBuilder()
                    .setId(seq.getId())
                    .setName(getSequenceName(seq.getId()))
                    .setSequence(new String(seq.getSequence()))
                    .build();
            listBuilder.addSeq(dto);
            count++;
        }
        lastAccessed = System.currentTimeMillis();
        return listBuilder.build();
    }

    @Override
    public boolean isFinished() {
        lastAccessed = System.currentTimeMillis();
        return !reader.hasMoreElements();
    }

    @Override
    public String getProjectName() {
        lastAccessed = System.currentTimeMillis();
        return projectName;
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

    private File getStorageFile(long run_id) throws MGXException {
        StringBuilder fname = new StringBuilder(mgxconfig.getPersistentDirectory())
                .append(File.separator).append(getProjectName())
                .append(File.separator).append("seqruns")
                .append(File.separator).append(run_id);

        File ret = new File(fname.toString());
        if (!ret.exists()) {
            throw new MGXException("Missing storage file for run " + run_id);
        }
        return ret;
    }

    private String getSequenceName(long seqId) throws MGXException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT name from read WHERE id=?")) {
            stmt.setLong(1, seqId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new MGXException("Could not obtain sequence name for id " + seqId);
    }
}
