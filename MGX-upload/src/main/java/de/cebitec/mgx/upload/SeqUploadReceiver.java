package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.qc.Analyzer;
import de.cebitec.mgx.qc.QCFactory;
import de.cebitec.mgx.qc.io.Persister;
import de.cebitec.mgx.seqstorage.CSFWriter;
import de.cebitec.mgx.seqstorage.DNASequence;
import de.cebitec.mgx.seqstorage.QualityDNASequence;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.sequence.SeqWriterI;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqUploadReceiver implements UploadReceiverI<SequenceDTOList> {
//
//    @EJB // (lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")

    MGXConfiguration mgxconfig;
    //
    protected final String projectName;
    protected final long runId;
    protected final Connection conn;
    protected File file;
    protected long total_num_sequences = 0;
    protected long lastAccessed;
    protected final Analyzer[] qcAnalyzers;
    protected final int bulksize;
    //
    private final BlockingQueue<DNASequenceI> queue = new LinkedBlockingQueue<>(10000);
    private final SeqFlusher flush;

    public SeqUploadReceiver(Executor executor, MGXConfiguration mgxcfg, Connection pConn, String projName, long run_id, boolean hasQuality) throws MGXException {
        projectName = projName;
        runId = run_id;
        mgxconfig = mgxcfg;

        try {
            //mgxconfig = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration");
            file = getStorageFile(run_id);
            SeqWriterI writer = new CSFWriter(file);
            conn = pConn;
            conn.setClientInfo("ApplicationName", "MGX-SeqUpload (" + projName + ")");
            qcAnalyzers = QCFactory.getQCAnalyzers(hasQuality);
            bulksize = mgxconfig.getSQLBulkInsertSize();
            flush = new SeqFlusher(run_id, queue, conn, writer, qcAnalyzers, bulksize);
            executor.execute(flush);
        } catch (MGXException | IOException | SeqStoreException | SQLClientInfoException ex) {
            throw new MGXException("Could not initialize sequence upload: " + ex.getMessage());
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public synchronized void add(SequenceDTOList seqs) throws MGXException {
        //
        throwIfError();
        //
        for (Iterator<SequenceDTO> iter = seqs.getSeqList().iterator(); iter.hasNext();) {
            SequenceDTO s = iter.next();
            DNASequenceI d;
            if (s.hasQuality()) {
                QualityDNASequence qd = new QualityDNASequence();
                qd.setQuality(s.getQuality().toByteArray());
                d = qd;
            } else {
                d = new DNASequence();
            }
            d.setName(s.getName().getBytes());
            d.setSequence(s.getSequence().getBytes());
            try {
                queue.put(d);
            } catch (InterruptedException ex) {
                Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
                throw new MGXException(ex);
            }
            total_num_sequences++;
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void close() throws MGXException {
        throwIfError();

        try {
            // commit pending data
            flush.complete();

            String sql = "UPDATE seqrun SET dbfile=?, num_sequences=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, file.getCanonicalPath());
                stmt.setLong(2, total_num_sequences);
                stmt.setLong(3, runId);
                stmt.executeUpdate();
            }
            //
            conn.setClientInfo("ApplicationName", "");
        } catch (Exception ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            cancel();
            throw new MGXException(ex.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // write QC stats
        String prefix = new StringBuilder(mgxconfig.getPersistentDirectory())
                .append(File.separator).append(getProjectName())
                .append(File.separator).append("QC")
                .append(File.separator).append(runId).append(".").toString();
        for (Analyzer a : qcAnalyzers) {
            Persister.persist(prefix, a);
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        try {
            flush.complete();
            SeqReaderFactory.delete(file.getCanonicalPath().toString());
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
                stmt.setLong(1, runId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                stmt.setLong(1, runId);
                stmt.executeUpdate();
            }
            //
            conn.setClientInfo("ApplicationName", "");
        } catch (Exception ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

    private File getStorageFile(long run_id) throws MGXException {
        StringBuilder fname = new StringBuilder(mgxconfig.getPersistentDirectory())
                .append(File.separator).append(getProjectName())
                .append(File.separator).append("seqruns")
                .append(File.separator);

        // create the directory tree
        File dirTree = new File(fname.toString());
        if (!dirTree.exists()) {
            dirTree.mkdirs();
            try {
                UnixHelper.makeDirectoryGroupWritable(fname.toString());
            } catch (IOException ex) {
                throw new MGXException(ex);
            }
        }

        fname.append(run_id);
        return new File(fname.toString());
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public long getSeqRunId() {
        return runId;
    }

    private void throwIfError() throws MGXException {
        if (flush.error()) {
            throw new MGXException(flush.getError());
        }
    }
}
