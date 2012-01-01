package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.seqstorage.CSFWriter;
import de.cebitec.mgx.seqstorage.DNASequence;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqWriterI;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SeqUploadReceiver implements UploadReceiverI<SequenceDTOList> {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    protected String projectName;
    protected long runId;
    protected Connection conn;
    protected SeqWriterI writer;
    protected File file;
    protected List<DNASequenceI> seqholder;
    protected long lastAccessed;
    protected int bulksize;
    
    public SeqUploadReceiver(Connection pConn, String projName, long run_id) throws MGXException {
        projectName = projName;
        runId = run_id;

        seqholder = new ArrayList<DNASequenceI>();

        try {
            mgxconfig = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration");
            file = getStorageFile(run_id);
            writer = new CSFWriter(file);
            conn = pConn;
            conn.setClientInfo("ApplicationName", "MGX-SeqUpload (" + projName + ")");
        } catch (Exception ex) {
            throw new MGXException("Could not initialize sequence upload: " + ex.getMessage());
        }

        bulksize = mgxconfig.getSQLBulkInsertSize();

        lastAccessed = System.currentTimeMillis();
    }

//    public SeqUploadReceiver(String jdbcUrl, String projName, long run_id) throws MGXException {
//        projectName = projName;
//        runId = run_id;
//
//        seqholder = new ArrayList<DNASequenceI>();
//
//        try {
//            mgxconfig = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration");
//            file = getStorageFile(run_id);
//            writer = new CSFWriter(file);
//            // FIXME use connection from pool
//            conn = DriverManager.getConnection(jdbcUrl, mgxconfig.getMGXUser(), mgxconfig.getMGXPassword());
//            conn.setClientInfo("ApplicationName", "MGX-SeqUpload (" + projName + ")");
//        } catch (Exception ex) {
//            throw new MGXException("Could not initialize sequence upload: " + ex.getMessage());
//        }
//
//        bulksize = mgxconfig.getSQLBulkInsertSize();
//
//        lastAccessed = System.currentTimeMillis();
//    }

    @Override
    public void add(SequenceDTOList seqs) throws MGXException {
        //int cnt = 0;
        for (SequenceDTO s : seqs.getSeqList()) {
            DNASequenceI d = new DNASequence();
            d.setName(s.getName().getBytes());
            d.setSequence(s.getSequence().getBytes());
            seqholder.add(d);
            //  cnt++;
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

        List<DNASequenceI> commitList = fetchChunk();

        String sql = createSQLBulkStatement(commitList.size());
        // insert sequence names and fetch list of generated ids
        List<Long> generatedIDs = new ArrayList<Long>(commitList.size());
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = conn.prepareStatement(sql);

            int i = 1;
            for (DNASequenceI s : commitList) {
                stmt.setLong(i, runId);
                stmt.setString(i + 1, new String(s.getName()));
                i += 2;
            }

            res = stmt.executeQuery();
            while (res.next()) {
                generatedIDs.add(res.getLong(1));
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            try {
                res.close();
                stmt.close();
            } catch (SQLException ex) {
                Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // write sequences to persistent storage
        ListIterator<Long> IDList = generatedIDs.listIterator();
        try {
            for (DNASequenceI s : commitList) {
                // add the generated IDs for the persistent file
                s.setId(IDList.next());
                writer.addSequence(s);
            }
        } catch (IOException ex) {
            throw new MGXException(ex);
        }
    }

    @Override
    public void close() throws MGXException {
        PreparedStatement stmt = null;
        try {
            // commit pending data
            while (seqholder.size() > 0) {
                flushCache();
            }
            writer.close();

            String sql = "UPDATE seqrun SET dbfile=? WHERE id=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, file.getCanonicalPath().toString());
            stmt.setLong(2, runId);
            stmt.executeUpdate();
            //
            conn.setClientInfo("ApplicationName", "");
        } catch (Exception ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            try {
                conn.close();
                stmt.close();
            } catch (SQLException ex) {
                Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        PreparedStatement stmt = null;
        try {
            writer.close();
            SeqReaderFactory.delete(file.getCanonicalPath().toString());
            stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?");
            stmt.setLong(1, runId);
            stmt.executeUpdate();
            //
            conn.setClientInfo("ApplicationName", "");
        } catch (Exception ex) {
            Logger.getLogger(SeqUploadReceiver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stmt.close();
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
        }

        fname.append(run_id);
        return new File(fname.toString());
    }

    private List<DNASequenceI> fetchChunk() {
        int chunk = seqholder.size() < bulksize ? seqholder.size() : bulksize;
        List<DNASequenceI> sub = seqholder.subList(0, chunk);
        List<DNASequenceI> subList = new ArrayList<DNASequenceI>(sub);
        sub.clear(); // since sub is backed by seqholder, this removes all sub-list items from seqholder
        return subList;
    }

    private String createSQLBulkStatement(int elements) {
        // build sql bulk insert statement
        StringBuilder sql = new StringBuilder("INSERT INTO read (seqrun_id, name) VALUES ");
        for (int cnt = 1; cnt <= elements; cnt++) {
            sql.append("(?,?),");
        }
        sql.deleteCharAt(sql.toString().length() - 1); // remove trailing ","

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
}
