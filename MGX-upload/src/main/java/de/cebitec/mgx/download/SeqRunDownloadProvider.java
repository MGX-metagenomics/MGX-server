package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqRunDownloadProvider implements DownloadProviderI<SequenceDTOList> {

    @EJB
    MGXConfigurationI mgxconfig;
    protected final String projectName;
    protected final DataSource dataSource;
    protected SeqReaderI<? extends DNASequenceI> reader;
    protected long lastAccessed;
    protected int bulksize;

    public SeqRunDownloadProvider(DataSource dataSource, String projName, long run_id) throws MGXException {
        this(dataSource, projName);
        File file = getStorageFile(run_id);
        try {
            reader = SeqReaderFactory.<DNASequenceI>getReader(file.getAbsolutePath());
        } catch (SeqStoreException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }
    }

    protected SeqRunDownloadProvider(DataSource dataSource, String projName) throws MGXException {
        projectName = projName;

        try {
            assert mgxconfig == null;
            mgxconfig = InitialContext.<MGXConfigurationI>doLookup("java:global/MGX-maven-ear/MGX-configuration-1.0-SNAPSHOT/MGXConfiguration!de.cebitec.mgx.configuration.api.MGXConfigurationI");

            this.dataSource = dataSource;
            //conn.setClientInfo("ApplicationName", "MGX-SeqDownload (" + projName + ")");
        } catch (NamingException ex) {
            throw new MGXException("Could not initialize sequence download: " + ex.getMessage());
        }

        bulksize = mgxconfig.getSQLBulkInsertSize();
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
//            conn.setClientInfo("ApplicationName", "");
//            conn.close();
        } catch (Exception ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() throws MGXException {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
//            conn.setClientInfo("ApplicationName", "");
//            conn.close();
        } catch (Exception ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SequenceDTOList fetch() throws MGXException {
        Builder listBuilder = SequenceDTOList.newBuilder();
        int count = 0;
        try {
            while (count <= bulksize && reader.hasMoreElements()) {
                DNASequenceI seq = reader.nextElement();
                String seqName = getSequenceName(seq.getId());
                if (seqName != null) {
                    SequenceDTO.Builder dtob = SequenceDTO.newBuilder()
                            .setId(seq.getId())
                            .setName(seqName)
                            .setSequence(new String(seq.getSequence()));

                    if (seq instanceof DNAQualitySequenceI) {
                        DNAQualitySequenceI qseq = (DNAQualitySequenceI) seq;
                        dtob.setQuality(ByteString.copyFrom(qseq.getQuality()));
                    }
                    listBuilder.addSeq(dtob.build());
                    count++;
                }
            }
            lastAccessed = System.currentTimeMillis();
            listBuilder.setComplete(!reader.hasMoreElements());
            return listBuilder.build();
        } catch (SeqStoreException ex) {
            throw new MGXException(ex);
        }
    }

    @Override
    public boolean isFinished() {
        lastAccessed = System.currentTimeMillis();
        try {
            return !reader.hasMoreElements();
        } catch (SeqStoreException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
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
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name from read WHERE id=?")) {
                stmt.setLong(1, seqId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    } else {
                        throw new MGXException("No name for read id " + seqId + " in project " + projectName);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SeqRunDownloadProvider.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
    }
}
