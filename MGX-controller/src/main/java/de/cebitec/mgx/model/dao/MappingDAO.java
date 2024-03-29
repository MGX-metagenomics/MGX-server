package de.cebitec.mgx.model.dao;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.workers.DeleteMapping;
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
public class MappingDAO extends DAO<Mapping> {

    public MappingDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Mapping> getType() {
        return Mapping.class;
    }

    private final static String CREATE = "INSERT INTO mapping (ref_id, run_id, job_id, bam_file) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Mapping obj) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setLong(1, obj.getReferenceId());
                stmt.setLong(2, obj.getSeqRunId());
                stmt.setLong(3, obj.getJobId());
                stmt.setString(4, obj.getBAMFile());

                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    private final static String BY_ID = "SELECT m.id, m.run_id, m.ref_id, m.job_id, m.bam_file "
            + "FROM mapping m WHERE m.id=?";

    @Override
    public Result<Mapping> getById(final long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("No object of type Mapping for ID " + id + ".");
                    }
                    Mapping m = new Mapping();
                    m.setId(rs.getLong(1));
                    m.setSeqrunId(rs.getLong(2));
                    m.setReferenceId(rs.getLong(3));
                    m.setJobId(rs.getLong(4));
                    m.setBAMFile(rs.getString(5));
                    return Result.ok(m);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private final static String FETCHALL = "SELECT m.id, m.run_id, m.ref_id, m.job_id, m.bam_file "
            + "FROM mapping m";

    public Result<AutoCloseableIterator<Mapping>> getAll() {
        List<Mapping> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {

                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {

                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        Mapping m = new Mapping();
                        m.setId(rs.getLong(1));
                        m.setSeqrunId(rs.getLong(2));
                        m.setReferenceId(rs.getLong(3));
                        m.setJobId(rs.getLong(4));
                        m.setBAMFile(rs.getString(5));

                        ret.add(m);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        ForwardingIterator<Mapping> iter = new ForwardingIterator<>(ret == null ? null : ret.iterator());
        return Result.ok(iter);
    }

    private final static String SQL_BY_SEQRUN = "SELECT m.id, m.ref_id, m.job_id, m.bam_file "
            + "FROM seqrun s "
            + "LEFT JOIN mapping m ON (m.run_id=s.id) "
            + "WHERE s.id=?";

    public Result<AutoCloseableIterator<Mapping>> bySeqRun(final long run_id) {
        if (run_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        List<Mapping> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BY_SEQRUN)) {
                stmt.setLong(1, run_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type SeqRun for ID " + run_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Mapping m = new Mapping();
                            m.setId(rs.getLong(1));
                            m.setSeqrunId(run_id);
                            m.setReferenceId(rs.getLong(2));
                            m.setJobId(rs.getLong(3));
                            m.setBAMFile(rs.getString(4));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(m);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        ForwardingIterator<Mapping> iter = new ForwardingIterator<>(ret == null ? null : ret.iterator());
        return Result.ok(iter);
    }

    private final static String SQL_BY_REFERENCE = "SELECT m.id, m.run_id, m.job_id, m.bam_file "
            + "FROM reference r "
            + "LEFT JOIN mapping m ON (m.ref_id=r.id) "
            + "WHERE r.id=?";

    public Result<AutoCloseableIterator<Mapping>> byReference(final long ref_id) {
        if (ref_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        List<Mapping> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BY_REFERENCE)) {
                stmt.setLong(1, ref_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type Reference for ID " + ref_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Mapping m = new Mapping();
                            m.setId(rs.getLong(1));
                            m.setSeqrunId(rs.getLong(2));
                            m.setReferenceId(ref_id);
                            m.setJobId(rs.getLong(3));
                            m.setBAMFile(rs.getString(4));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(m);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        ForwardingIterator<Mapping> iter = new ForwardingIterator<>(ret == null ? null : ret.iterator());
        return Result.ok(iter);
    }

    private final static String SQL_BY_JOB = "SELECT m.id, m.run_id, m.ref_id, m.bam_file "
            + "FROM job j "
            + "LEFT JOIN mapping m ON (m.job_id=j.id) "
            + "WHERE j.id=?";

    public Result<AutoCloseableIterator<Mapping>> byJob(final long job_id) {
        if (job_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }
        List<Mapping> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BY_JOB)) {
                stmt.setLong(1, job_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type Job for ID " + job_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            Mapping m = new Mapping();
                            m.setId(rs.getLong(1));
                            m.setSeqrunId(rs.getLong(2));
                            m.setReferenceId(rs.getLong(3));
                            m.setJobId(job_id);
                            m.setBAMFile(rs.getString(4));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(m);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        ForwardingIterator<Mapping> iter = new ForwardingIterator<>(ret == null ? null : ret.iterator());
        return Result.ok(iter);
    }

    public Result<TaskI> delete(long id, GPMSManagedDataSourceI ds, String projectName, String jobDir) {
        DeleteMapping del = new DeleteMapping(id, ds, projectName, jobDir);
        return Result.ok(del);
    }

}
