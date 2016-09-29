package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
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

    private final static String CREATE = "INSERT INTO seqrun (name, dbfile, database_accession, num_sequences, sequencing_method, sequencing_technology, submitted_to_insdc, dnaextract_id) "
            + "VALUES (?,?,?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(SeqRun obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getDBFile());
                stmt.setString(3, obj.getAccession());
                stmt.setLong(4, 0); //num seqs
                stmt.setLong(5, obj.getSequencingMethod());
                stmt.setLong(6, obj.getSequencingTechnology());
                stmt.setBoolean(7, obj.getSubmittedToINSDC());
                stmt.setLong(8, obj.getExtractId());

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

    public void delete(long run_id) throws MGXException {
//        final SeqRun sr = getById(run_id);
//        // remove persistent storage file
//        String dBFile = sr.getDBFile();
//        if (dBFile != null) {
//            SeqReaderFactory.delete(dBFile);
//        }

        Connection conn = null;
        PreparedStatement stmt = null;

        /*
         * JPA CascadeType.DELETE fetches and delete all entities individually;
         * we can do better by manually deleting all referring objects ..
         */
        try {
            conn = getConnection();

            // delete observations
            stmt = conn.prepareStatement("DELETE FROM observation WHERE seq_id IN (SELECT id FROM read WHERE seqrun_id=?)");
            stmt.setLong(1, run_id);
            stmt.execute();

            // delete attributecounts
            stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id IN "
                    + "(SELECT id from job WHERE seqrun_id=?)"
                    + ")");
            stmt.setLong(1, run_id);
            stmt.execute();

            // delete attributes
            stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id IN "
                    + "(SELECT id from job WHERE seqrun_id=?)");
            stmt.setLong(1, run_id);
            stmt.execute();
            // delete jobs
            //            stmt = conn.prepareStatement("DELETE FROM job WHERE seqrun_id=?");
            //            stmt.setLong(1, id);
            //            stmt.execute();
            Iterator<Job> iter = getController().getJobDAO().bySeqRun(run_id);
            while (iter.hasNext()) {
                getController().getJobDAO().delete(iter.next().getId());
            }

            // delete reads
            stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?");
            stmt.setLong(1, run_id);
            stmt.execute();
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, null);
        }

        // remove persistent entity
        try (Connection conn2 = getConnection()) {
            try (PreparedStatement ps = conn2.prepareStatement("DELETE FROM seqrun WHERE id=?")) {
                ps.setLong(1, run_id);
                int numRows = ps.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + run_id + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, null);
        }
    }

    private final static String BY_ID = "SELECT s.id, s.name, s.dbfile, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id "
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
                    s.setDBFile(rs.getString(3));
                    s.setAccession(rs.getString(4));
                    s.setNumberOfSequences(rs.getLong(5));
                    s.setSequencingMethod(rs.getLong(6));
                    s.setSequencingTechnology(rs.getLong(7));
                    s.setSubmittedToINSDC(rs.getBoolean(8));
                    s.setExtractId(rs.getLong(9));
                    return s;

                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    private final static String FETCHALL = "SELECT s.id, s.name, s.dbfile, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc, s.dnaextract_id "
            + "FROM seqrun s";

    public AutoCloseableIterator<SeqRun> getAll() throws MGXException {

        List<SeqRun> ret = null;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        SeqRun s = new SeqRun();
                        s.setId(rs.getLong(1));
                        s.setName(rs.getString(2));
                        s.setDBFile(rs.getString(3));
                        s.setAccession(rs.getString(4));
                        s.setNumberOfSequences(rs.getLong(5));
                        s.setSequencingMethod(rs.getLong(6));
                        s.setSequencingTechnology(rs.getLong(7));
                        s.setSubmittedToINSDC(rs.getBoolean(8));
                        s.setExtractId(rs.getLong(9));

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

    private final static String SQL_BY_EXTRACT = "SELECT s.id, s.name, s.dbfile, s.database_accession, s.num_sequences, s.sequencing_method, "
            + "s.sequencing_technology, s.submitted_to_insdc "
            + "FROM dnaextract d LEFT JOIN seqrun s on (d.id=s.dnaextract_id) WHERE d.id=?";

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
                            s.setDBFile(rs.getString(3));
                            s.setAccession(rs.getString(4));
                            s.setNumberOfSequences(rs.getLong(5));
                            s.setSequencingMethod(rs.getLong(6));
                            s.setSequencingTechnology(rs.getLong(7));
                            s.setSubmittedToINSDC(rs.getBoolean(8));

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

    private final static String UPDATE = "UPDATE seqrun SET name=?, database_accession=?, sequencing_method=?,  sequencing_technology=?, submitted_to_insdc=? "
            + "WHERE id=?";

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
                stmt.setLong(6, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }
}
