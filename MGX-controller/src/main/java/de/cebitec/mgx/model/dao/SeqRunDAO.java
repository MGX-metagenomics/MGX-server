package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.workers.DeleteSeqRun;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class SeqRunDAO extends DAO<SeqRun> {

    public SeqRunDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<SeqRun> getType() {
        return SeqRun.class;
    }

    private final static String CREATE = "INSERT INTO seqrun (name, database_accession, num_sequences, sequencing_method, sequencing_technology, submitted_to_insdc, dnaextract_id, paired) "
            + "VALUES (?,?,?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(SeqRun obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getAccession());
                stmt.setLong(3, -1L); //num seqs
                stmt.setLong(4, obj.getSequencingMethod());
                stmt.setLong(5, obj.getSequencingTechnology());
                stmt.setBoolean(6, obj.getSubmittedToINSDC());
                stmt.setLong(7, obj.getExtractId());
                stmt.setBoolean(8, obj.isPaired());

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

    public Result<TaskI> delete(long runId) throws IOException {
        Result<File> dbFile = getDBFile(runId);
        if (dbFile.isError()) {
            return Result.error(dbFile.getError());
        }
        String dbFilepath = dbFile.getValue().getAbsolutePath();

        TaskI t = new DeleteSeqRun(runId,
                getController().getDataSource(),
                getController().getProjectName(),
                getController().getProjectDirectory(),
                dbFilepath);
        return Result.ok(t);
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
    public Result<SeqRun> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type SeqRun for ID " + id + ".");
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
                    return Result.ok(s);

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    public Result<AutoCloseableIterator<SeqRun>> getByIds(long... ids) {
        if (ids == null || ids.length == 0) {
            return Result.error("Null/empty ID list.");
        }
        String BY_IDS = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
                + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
                + "FROM seqrun s WHERE s.id IN (" + toSQLTemplateString(ids.length) + ")";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_IDS);
            int idx = 1;
            for (long id : ids) {
                if (id <= 0) {
                    return Result.error("No/Invalid ID supplied.");
                }
                stmt.setLong(idx++, id);
            }
            ResultSet rs = stmt.executeQuery();

            DBIterator<SeqRun> dbIterator = new DBIterator<SeqRun>(rs, stmt, conn) {
                @Override
                public SeqRun convert(ResultSet rs) throws SQLException {
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
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String FETCHALL = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s WHERE s.num_sequences <> -1";

    public Result<AutoCloseableIterator<SeqRun>> getAll() {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();

            DBIterator<SeqRun> dbIterator = new DBIterator<SeqRun>(rs, stmt, conn) {
                @Override
                public SeqRun convert(ResultSet rs) throws SQLException {
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
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private final static String BY_JOB = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s WHERE s.id IN (SELECT unnest(seqruns) FROM job WHERE id=?)";

    public Result<AutoCloseableIterator<SeqRun>> byJob(long jobId) {
        if (jobId <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_JOB);
            stmt.setLong(1, jobId);
            ResultSet rs = stmt.executeQuery();

            DBIterator<SeqRun> dbIterator = new DBIterator<SeqRun>(rs, stmt, conn) {
                @Override
                public SeqRun convert(ResultSet rs) throws SQLException {
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
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String SQL_BY_EXTRACT
            = "SELECT id, name, database_accession, num_sequences, sequencing_method, "
            + "sequencing_technology, submitted_to_insdc, paired, dnaextract_id FROM seqrun WHERE num_sequences <> -1 "
            + "AND dnaextract_id=?";

    public Result<AutoCloseableIterator<SeqRun>> byDNAExtract(final long extract_id) {
        if (extract_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(SQL_BY_EXTRACT);
            stmt.setLong(1, extract_id);
            ResultSet rs = stmt.executeQuery();

            DBIterator<SeqRun> dbIterator = new DBIterator<SeqRun>(rs, stmt, conn) {
                @Override
                public SeqRun convert(ResultSet rs) throws SQLException {
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
                    return s;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String RUNS_BY_ASSEMBLY = "SELECT s.id, s.name, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id, s.paired "
            + "FROM seqrun s "
            + "WHERE s.id=ANY("
            + "(SELECT j.seqruns FROM assembly a LEFT JOIN job j ON (a.job_id=j.id) WHERE a.id=?)::BIGINT[])";

    public Result<AutoCloseableIterator<SeqRun>> byAssembly(final long asmId) {

        if (asmId <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(RUNS_BY_ASSEMBLY);
            stmt.setLong(1, asmId);
            ResultSet rs = stmt.executeQuery();

            DBIterator<SeqRun> dbIterator = new DBIterator<SeqRun>(rs, stmt, conn) {
                @Override
                public SeqRun convert(ResultSet rs) throws SQLException {
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
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String UPDATE = "UPDATE seqrun SET name=?, database_accession=?, sequencing_method=?,  sequencing_technology=?, submitted_to_insdc=?, "
            + "paired=? WHERE id=?";

    public void update(SeqRun obj) throws MGXException {
        if (obj.getId() == SeqRun.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type SeqRun without an ID.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getAccession());
                stmt.setLong(3, obj.getSequencingMethod());
                stmt.setLong(4, obj.getSequencingTechnology());
                stmt.setBoolean(5, obj.getSubmittedToINSDC());
                stmt.setBoolean(6, obj.isPaired());
                stmt.setLong(7, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type SeqRun for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public boolean hasQuality(long id) throws IOException, MGXException {
        Result<File> dbFile = getDBFile(id);
        if (dbFile.isError()) {
            throw new MGXException(dbFile.getError());
        }
        return new File(dbFile.getValue().getAbsolutePath() + ".csq").exists();
    }

    public Result<File> getDBFile(long id) throws IOException {
        if (id <= 0) {
            return Result.error("Invalid seqrun ID: " + id);
        }
        File f = new File(getController().getProjectSeqRunDirectory().getAbsolutePath() + File.separator + id);
        return Result.ok(f);
    }

}
