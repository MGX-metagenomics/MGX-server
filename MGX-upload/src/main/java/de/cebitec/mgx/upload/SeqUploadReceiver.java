package de.cebitec.mgx.upload;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.qc.Analyzer;
import de.cebitec.mgx.qc.QCFactory;
import de.cebitec.mgx.qc.io.Persister;
import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.seqstorage.CSFWriter;
import de.cebitec.mgx.seqstorage.CSQFWriter;
import de.cebitec.mgx.seqstorage.EncodedDNASequence;
import de.cebitec.mgx.seqstorage.EncodedQualityDNASequence;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.sequence.SeqWriterI;
import de.cebitec.mgx.util.UnixHelper;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqUploadReceiver<T extends DNASequenceI> implements UploadReceiverI<SequenceDTOList> {

    protected final String projectName;
    protected final long runId;
    protected final boolean isPairedEnd;
    protected GPMSManagedDataSourceI dataSource;
    protected final File projectDirectory;
    protected final File projectQCDirectory;
    protected final File file;
    protected long total_num_sequences = 0;
    protected long lastAccessed;
    protected final Analyzer<T>[] qcAnalyzers;
    //
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(100_000);
    private final SeqFlusher<T> flush;

    @SuppressWarnings("unchecked")
    public SeqUploadReceiver(Executor executor, File projectDir, File projectQCDir, GPMSManagedDataSourceI dataSource, String projName, long run_id, boolean hasQuality, boolean isPaired) throws MGXException {
        this.projectName = projName;
        this.runId = run_id;
        this.isPairedEnd = isPaired;
        this.projectDirectory = projectDir;
        this.projectQCDirectory = projectQCDir;
        this.dataSource = dataSource;

        dataSource.subscribe(this);

        try {
            file = getStorageFile(run_id);
            SeqWriterI writer = hasQuality ? new CSQFWriter(file) : new CSFWriter(file);
            qcAnalyzers = QCFactory.<T>getQCAnalyzers(hasQuality, isPaired);
            flush = new SeqFlusher<>(run_id, isPaired, queue, dataSource, writer, qcAnalyzers);
            executor.execute(flush);
        } catch (MGXException | IOException | SeqStoreException ex) {
            throw new MGXException("Could not initialize sequence upload: " + ex.getMessage());
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void add(SequenceDTOList seqs) throws MGXException {
        //
        throwIfError();
        //
        
        //
        // for paired-end data, each chunk has to contain an even number of sequences
        //
        if (isPairedEnd) {
            if (seqs.getSeqCount() % 2 != 0) {
                throw new MGXException("Invalid data chunk with unbalanced forward/reverse reads, got "+ seqs.getSeqCount() + " sequences in chunk.");
            }
        }
        
        try {
            for (SequenceDTO s : seqs.getSeqList()) {
                if (s.getName().length() > 255) {
                    throw new MGXException("Sequence name too long, max 255 characters supported.");
                }

                if (!s.getQuality().isEmpty()) {
                    EncodedQualityDNASequence qd = new EncodedQualityDNASequence(-1, s.getSequence().toByteArray(), s.getQuality().toByteArray(), true);
                    qd.setName(s.getName().getBytes());
                    queue.put((T) qd);
                } else {
                    EncodedDNASequence eds = new EncodedDNASequence(-1, s.getSequence().toByteArray(), true);
                    eds.setName(s.getName().getBytes());
                    queue.put((T) eds);
                }

                total_num_sequences++;
            }
        } catch (InterruptedException | SequenceException ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void close() throws MGXException {
        throwIfError();

        try {
            // commit pending data
            flush.complete();

            String sql = "UPDATE seqrun SET num_sequences=? WHERE id=?";
            try (Connection conn = dataSource.getConnection(this)) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, total_num_sequences);
                    stmt.setLong(2, runId);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            cancel();
            throw new MGXException(ex.getMessage());
        }

        if (dataSource != null) {
            dataSource.close(this);
            dataSource = null;
        }

        // write QC stats
        String prefix = new StringBuilder(projectQCDirectory.getAbsolutePath())
                .append(File.separator).append(runId).append(".").toString();
        for (Analyzer<?> a : qcAnalyzers) {
            Persister.persist(prefix, a);
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        System.err.println("Upload cancelled after " + total_num_sequences + " sequences, queue size " + queue.size());
        try {
            flush.complete();
            SeqReaderFactory.delete(file.getCanonicalPath());

            try (Connection conn = dataSource.getConnection(this)) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
                    stmt.setLong(1, runId);
                    stmt.executeUpdate();
                }
            }

            try (Connection conn = dataSource.getConnection(this)) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                    stmt.setLong(1, runId);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (dataSource != null) {
            dataSource.close(this);
            dataSource = null;
        }
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

    private File getStorageFile(long run_id) throws MGXException {
        StringBuilder fname = new StringBuilder(projectDirectory.getAbsolutePath())
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
