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
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
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
public class SeqUploadReceiver implements UploadReceiverI<SequenceDTOList> {

    @EJB // (lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    //
    private final Executor executor;
    protected final String projectName;
    protected final long runId;
    protected final Connection conn;
    protected SeqWriterI writer;
    protected File file;
    protected List<DNASequenceI> seqholder;
    protected long total_num_sequences = 0;
    protected long lastAccessed;
    protected final Analyzer[] qcAnalyzers;
    protected int bulksize;
    //
    private volatile MGXException lastException = null;

    public SeqUploadReceiver(Executor executor, Connection pConn, String projName, long run_id, boolean hasQuality) throws MGXException {
        this.executor = executor;
        projectName = projName;
        runId = run_id;

        try {
            mgxconfig = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration");
            file = getStorageFile(run_id);
            writer = new CSFWriter(file);
            conn = pConn;
            conn.setClientInfo("ApplicationName", "MGX-SeqUpload (" + projName + ")");
        } catch (NamingException | MGXException | IOException | SeqStoreException | SQLClientInfoException ex) {
            throw new MGXException("Could not initialize sequence upload: " + ex.getMessage());
        }

        qcAnalyzers = QCFactory.getQCAnalyzers(hasQuality);
        seqholder = new ArrayList<>();
        bulksize = mgxconfig.getSQLBulkInsertSize();
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void add(SequenceDTOList seqs) throws MGXException {
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
            seqholder.add(d);
            total_num_sequences++;
        }

        while (seqholder.size() >= bulksize) {
            flushChunk();
        }

        lastAccessed = System.currentTimeMillis();
    }

    protected void flushCache() throws MGXException {
        while (seqholder.size() > 0) {
            flushChunk();
            lastAccessed = System.currentTimeMillis();
        }
    }

    protected void flushChunk() throws MGXException {

        final List<? extends DNASequenceI> commitList = fetchChunk();

        executor.execute(new Runnable() {

            @Override
            public void run() {
                String sql = createSQLBulkStatement(commitList.size());
                // insert sequence names and fetch list of generated ids

                int curPos = 0;
                long[] generatedIDs = new long[commitList.size()];

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                    int i = 1;
                    for (DNASequenceI s : commitList) {
                        stmt.setLong(i, runId);
                        stmt.setString(i + 1, new String(s.getName()));
                        stmt.setInt(i + 2, s.getSequence().length);
                        i += 3;
                    }

                    try (ResultSet res = stmt.executeQuery()) {
                        while (res.next()) {
                            generatedIDs[curPos++] = res.getLong(1);
                        }
                    }
                } catch (SQLException ex) {
                    lastException = new MGXException(ex.getMessage());
                    return;
                }

                //
                // write sequences to persistent storage using the generated ids
                //
                curPos = 0;
                try {
                    for (Iterator<? extends DNASequenceI> iter = commitList.iterator(); iter.hasNext();) {
                        // add the generated IDs
                        DNASequenceI s = iter.next();
                        s.setId(generatedIDs[curPos++]);
                        writer.addSequence(s);
                        for (Analyzer a : qcAnalyzers) {
                            a.add(s);
                        }
                    }
                } catch (IOException ex) {
                    lastException = new MGXException(ex);
                    return;
                }
            }
        });

    }

    @Override
    public void close() throws MGXException {
        throwIfError();
        try {
            // commit pending data
            while (seqholder.size() > 0) {
                flushCache();
            }
            writer.close();

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
            Persister.persist(prefix, a.get());
        }
        
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        try {
            writer.close();
            SeqReaderFactory.delete(file.getCanonicalPath().toString());
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?")) {
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
            UnixHelper.makeDirectoryGroupWritable(fname.toString());
        }

        fname.append(run_id);
        return new File(fname.toString());
    }

    private List<? extends DNASequenceI> fetchChunk() {
        int chunk = seqholder.size() < bulksize ? seqholder.size() : bulksize;
        List<? extends DNASequenceI> sub = seqholder.subList(0, chunk);
        List<? extends DNASequenceI> subList = new ArrayList<>(sub);
        sub.clear(); // since sub is backed by seqholder, this removes all sub-list items from seqholder
        return subList;
    }

    private String createSQLBulkStatement(int elements) {
        // build sql bulk insert statement
        StringBuilder sql = new StringBuilder("INSERT INTO read (seqrun_id, name, length) VALUES ");
        for (int cnt = 1; cnt <= elements; cnt++) {
            sql.append("(?,?,?),");
        }
        sql.deleteCharAt(sql.length() - 1); // remove trailing ","

        /*
         * stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
         * ResultSet res = stmt.getGeneratedKeys();
         *
         * leads to 'RETURNING *' internally,
         *
         * therefore we restrict to id column to minimize amount of data
         * that needs to be transferred
         */
        sql.append(" RETURNING id");
        return sql.toString();
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public long getSeqRunId() {
        return runId;
    }

    private void throwIfError() throws MGXException {
        if (lastException != null) {
            MGXException ex = lastException;
            lastException = null;
            throw ex;
        }
    }
}
