package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteSeqRun;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class SeqRunDAO extends DAO<SeqRun> {

    public SeqRunDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return SeqRun.class;
    }

    private final static String CREATE = "INSERT INTO seqrun (name, database_accession, num_sequences, sequencing_method, sequencing_technology, submitted_to_insdc, dnaextract_id, paired) "
            + "VALUES (?,?,?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(SeqRun obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getAccession());
                stmt.setLong(3, -1L); //num seqs
                stmt.setLong(4, obj.getSequencingMethod());
                stmt.setLong(5, obj.getSequencingTechnology());
                stmt.setBoolean(6, obj.getSubmittedToINSDC());
                stmt.setLong(7, obj.getExtractId());
                stmt.setBoolean(8, obj.isPaired());

                try (ResultSet rs = stmt.executeQuery()) {
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

    public TaskI delete(long runId) throws MGXException, IOException {
        String dbFile = getDBFile(runId).getAbsolutePath();
        TaskI t = new DeleteSeqRun(runId,
                getController().getDataSource(),
                getController().getProjectName(),
                getController().getProjectDirectory(),
                dbFile);
        return t;
    }

//    public void delete(long run_id) throws MGXException {
//
//        try {
//            // delete observations
//            //
//            // delete in chunks to make sure the DB connections gets returned
//            // to the pool in a timely manner
//            //
//            final String delObs = "DELETE FROM observation WHERE ctid = any(array(SELECT ctid FROM observation WHERE seq_id IN (SELECT id FROM read WHERE seqrun_id=?) LIMIT 5000))";
//            int rowsAffected;
//            do {
//                try (Connection c = getConnection()) {
//                    try (PreparedStatement ps = c.prepareStatement(delObs)) {
//                        ps.setLong(1, run_id);
//                        rowsAffected = ps.executeUpdate();
//                    }
//                }
//            } while (rowsAffected == 5_000);
//
//            // delete attributecounts
//            try (Connection conn = getConnection()) {
//                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
//                        + "(SELECT id FROM attribute WHERE job_id IN "
//                        + "(SELECT id from job WHERE seqrun_id=?)"
//                        + ")")) {
//                    stmt.setLong(1, run_id);
//                    stmt.execute();
//                }
//            }
//
//            // delete attributes
//            try (Connection conn = getConnection()) {
//                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id IN "
//                        + "(SELECT id from job WHERE seqrun_id=?)")) {
//                    stmt.setLong(1, run_id);
//                    stmt.execute();
//                }
//            }
//            // delete jobs
//            //            stmt = conn.prepareStatement("DELETE FROM job WHERE seqrun_id=?");
//            //            stmt.setLong(1, id);
//            //            stmt.execute();
//            Iterator<Job> iter = getController().getJobDAO().bySeqRun(run_id);
//            while (iter.hasNext()) {
//                getController().getJobDAO().delete(iter.next().getId());
//            }
//
//            //
//            // delete in chunks to make sure the DB connections gets returned
//            // to the pool in a timely manner
//            //
//            String delReads = "DELETE FROM read WHERE ctid = any(array(SELECT ctid FROM read WHERE seqrun_id=? LIMIT 5000))";
//            do {
//                try (Connection conn = getConnection()) {
//                    try (PreparedStatement stmt = conn.prepareStatement(delReads)) {
//                        stmt.setLong(1, run_id);
//                        rowsAffected = stmt.executeUpdate();
//                    }
//                }
//            } while (rowsAffected == 5_000);
//        } catch (SQLException ex) {
//            throw new MGXException(ex.getMessage());
//        }
//
//        // remove persistent entity
//        try (Connection conn = getConnection()) {
//            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
//                stmt.setLong(1, run_id);
//                int numRows = stmt.executeUpdate();
//                if (numRows != 1) {
//                    throw new MGXException("No object of type " + getClassName() + " for ID " + run_id + ".");
//                }
//            }
//        } catch (SQLException ex) {
//            throw new MGXException(ex.getMessage());
//        }
//    }
    private final static String BY_ID = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s WHERE s.id=?";

    @Override
    public SeqRun getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }

                    SeqRun s = new SeqRun();
                    s.setId(rs.getLong(1));
                    s.setName(rs.getString(2));
                    s.setAccession(rs.getString(3));
                    s.setNumberOfSequences(rs.getLong(4));
                    s.setSequencingMethod(rs.getLong(5));
                    s.setSequencingTechnology(rs.getLong(6));
                    s.setSubmittedToINSDC(rs.getBoolean(7));
                    s.setExtractId(rs.getLong(8));
                    s.setIsPaired(rs.getBoolean(9));
                    return s;

                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public AutoCloseableIterator<SeqRun> getByIds(long... ids) throws MGXException {
        if (ids == null || ids.length == 0) {
            throw new MGXException("Null/empty ID list.");
        }
        List<SeqRun> ret = null;
        String BY_IDS = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
                + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
                + "FROM seqrun s WHERE s.id IN (" + toSQLTemplateString(ids.length) + ")";

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_IDS)) {
                int idx = 1;
                for (long id : ids) {
                    if (id <= 0) {
                        throw new MGXException("No/Invalid ID supplied.");
                    }
                    stmt.setLong(idx++, id);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        SeqRun s = new SeqRun();
                        s.setId(rs.getLong(1));
                        s.setName(rs.getString(2));
                        s.setAccession(rs.getString(3));
                        s.setNumberOfSequences(rs.getLong(4));
                        s.setSequencingMethod(rs.getLong(5));
                        s.setSequencingTechnology(rs.getLong(6));
                        s.setSubmittedToINSDC(rs.getBoolean(7));
                        s.setExtractId(rs.getLong(8));
                        s.setIsPaired(rs.getBoolean(9));
                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        ret.add(s);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());

    }

    private final static String FETCHALL = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s WHERE s.num_sequences <> -1";

    public AutoCloseableIterator<SeqRun> getAll() throws MGXException {

        List<SeqRun> ret = null;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        SeqRun s = new SeqRun();
                        s.setId(rs.getLong(1));
                        s.setName(rs.getString(2));
                        s.setAccession(rs.getString(3));
                        s.setNumberOfSequences(rs.getLong(4));
                        s.setSequencingMethod(rs.getLong(5));
                        s.setSequencingTechnology(rs.getLong(6));
                        s.setSubmittedToINSDC(rs.getBoolean(7));
                        s.setExtractId(rs.getLong(8));
                        s.setIsPaired(rs.getBoolean(9));

                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        ret.add(s);
                    }

                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String BY_JOB = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s WHERE s.id IN (SELECT unnest(seqruns) FROM job WHERE id=?)";

    public AutoCloseableIterator<SeqRun> byJob(long jobId) throws MGXException {
        if (jobId <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<SeqRun> ret = null;
        Job job = getController().getJobDAO().getById(jobId);

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_JOB)) {
                stmt.setLong(1, jobId);
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        SeqRun s = new SeqRun();
                        s.setId(rs.getLong(1));
                        s.setName(rs.getString(2));
                        s.setAccession(rs.getString(3));
                        s.setNumberOfSequences(rs.getLong(4));
                        s.setSequencingMethod(rs.getLong(5));
                        s.setSequencingTechnology(rs.getLong(6));
                        s.setSubmittedToINSDC(rs.getBoolean(7));
                        s.setExtractId(rs.getLong(8));
                        s.setIsPaired(rs.getBoolean(9));
                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        ret.add(s);
                    }

                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String SQL_BY_EXTRACT
            = "WITH complete_runs AS ("
            + "SELECT id, name, database_accession, num_sequences, sequencing_method, "
            + "sequencing_technology, submitted_to_insdc, paired, dnaextract_id FROM seqrun WHERE num_sequences <> -1"
            + ")"
            + "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.paired "
            + "FROM dnaextract d LEFT JOIN complete_runs s ON (d.id=s.dnaextract_id) WHERE d.id=?";

    public AutoCloseableIterator<SeqRun> byDNAExtract(final long extract_id) throws MGXException {
        if (extract_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<SeqRun> ret = null;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BY_EXTRACT)) {
                stmt.setLong(1, extract_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type DNAExtract for ID " + extract_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            SeqRun s = new SeqRun();
                            s.setExtractId(extract_id);
                            s.setId(rs.getLong(1));
                            s.setName(rs.getString(2));
                            s.setAccession(rs.getString(3));
                            s.setNumberOfSequences(rs.getLong(4));
                            s.setSequencingMethod(rs.getLong(5));
                            s.setSequencingTechnology(rs.getLong(6));
                            s.setSubmittedToINSDC(rs.getBoolean(7));
                            s.setIsPaired(rs.getBoolean(8));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(s);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    public AutoCloseableIterator<SeqRun> byAssembly(final long asmId) throws MGXException {
        // FIXME - 3 DB roundtrips
        Assembly asm = getController().getAssemblyDAO().getById(asmId);
        Job job = getController().getJobDAO().getById(asm.getAsmjobId());
        return getController().getSeqRunDAO().getByIds(job.getSeqrunIds());
    }

    private final static String UPDATE = "UPDATE seqrun SET name=?, database_accession=?, sequencing_method=?,  sequencing_technology=?, submitted_to_insdc=?, "
            + "paired=? WHERE id=?";

    public void update(SeqRun obj) throws MGXException {
        if (obj.getId() == SeqRun.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getAccession());
                stmt.setLong(3, obj.getSequencingMethod());
                stmt.setLong(4, obj.getSequencingTechnology());
                stmt.setBoolean(5, obj.getSubmittedToINSDC());
                stmt.setBoolean(6, obj.isPaired());
                stmt.setLong(7, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public boolean hasQuality(long id) throws IOException, MGXException {
        return new File(getDBFile(id).getAbsolutePath() + ".csq").exists();
    }

    public File getDBFile(long id) throws MGXException, IOException {
        if (id <= 0) {
            throw new MGXException("Invalid seqrun ID: " + id);
        }
        return new File(getController().getProjectSeqRunDirectory().getAbsolutePath() + File.separator + id);
    }

}
