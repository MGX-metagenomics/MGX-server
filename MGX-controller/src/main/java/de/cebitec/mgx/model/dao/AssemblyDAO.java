package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.workers.DeleteAssembly;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class AssemblyDAO extends DAO<Assembly> {

    public AssemblyDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Assembly> getType() {
        return Assembly.class;
    }

    private final static String CREATE = "INSERT INTO assembly (name, reads_assembled, n50, job_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Assembly obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getReadsAssembled());
                stmt.setLong(3, obj.getN50());
                stmt.setLong(4, obj.getAsmjobId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        try {
            File asmDir = getController().getProjectAssemblyDirectory();
            new File(asmDir, String.valueOf(obj.getId())).mkdir();
        } catch (IOException ex) {
            Logger.getLogger(AssemblyDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return obj.getId();
    }

    private final static String UPDATE = "UPDATE assembly SET name=?, reads_assembled=?, n50=?, job_id=? WHERE id=?";

    public void update(Assembly obj) throws MGXException {
        if (obj.getId() == Assembly.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type Assembly without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setLong(2, obj.getReadsAssembled());
                stmt.setLong(3, obj.getN50());
                stmt.setLong(4, obj.getAsmjobId());

                stmt.setLong(5, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type Assembly for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public Result<TaskI> delete(long id) {
        List<TaskI> subtasks = new ArrayList<>();

        Result<AutoCloseableIterator<Job>> jobs = getController().getJobDAO().byAssembly(id);
        if (jobs.isError()) {
            return Result.error(jobs.getError());
        }
        AutoCloseableIterator<Job> iter = jobs.getValue();
        while (iter != null && iter.hasNext()) {
            try {
                Result<TaskI> delete = getController().getJobDAO().delete(iter.next().getId());
                if (delete.isError()) {
                    return Result.error(delete.getError());
                }
                subtasks.add(delete.getValue());
            } catch (IOException ex) {
                Logger.getLogger(AssemblyDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Result<AutoCloseableIterator<Bin>> res = getController().getBinDAO().byAssembly(id);
        if (res.isError()) {
            return Result.error(res.getError());
        }
        
        try (AutoCloseableIterator<Bin> biter = res.getValue()) {
            while (biter.hasNext()) {
                Bin s = biter.next();
                Result<TaskI> del = getController().getBinDAO().delete(s.getId());
                if (del.isError()) {
                    return Result.error(del.getError());
                }
                subtasks.add(del.getValue());
            }
        }

        try {
            final File assemblyDir = new File(getController().getProjectAssemblyDirectory(), String.valueOf(id));
            if (assemblyDir.exists()) {
                TaskI delAssemblyDir = new TaskI("Deleting assembly directory", getController().getDataSource()) {
                    @Override
                    public void process() throws Exception {

                        assemblyDir.delete();
                    }
                };
                subtasks.add(delAssemblyDir);
            }
        } catch (IOException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        TaskI t = new DeleteAssembly(getController().getDataSource(), id, getController().getProjectName(), subtasks.toArray(new TaskI[]{}));
        return Result.ok(t);
    }

    private static final String FETCHALL = "SELECT id, name, reads_assembled, n50, job_id FROM assembly";
    private static final String BY_ID = "SELECT id, name, reads_assembled, n50, job_id FROM assembly WHERE id=?";

    @Override
    public Result<Assembly> getById(long id) {
        if (id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Assembly ret = new Assembly();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setReadsAssembled(rs.getLong(3));
                        ret.setN50(rs.getInt(4));
                        ret.setAsmjobId(rs.getLong(5));
                        return Result.ok(ret);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        return Result.error("No object of type Assembly for ID " + id + ".");
    }

    public Result<AutoCloseableIterator<Assembly>> getAll() {

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(FETCHALL);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Assembly> dbIterator = new DBIterator<Assembly>(rs, stmt, conn) {
                @Override
                public Assembly convert(ResultSet rs) throws SQLException {
                    Assembly ret = new Assembly();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setReadsAssembled(rs.getLong(3));
                    ret.setN50(rs.getInt(4));
                    ret.setAsmjobId(rs.getLong(5));
                    return ret;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

    private static final String BY_JOB_ID = "SELECT id, name, reads_assembled, n50 FROM assembly WHERE job_id=?";

    public Result<Assembly> byJob(long jobId) {
        if (jobId <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_JOB_ID)) {
                stmt.setLong(1, jobId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Assembly ret = new Assembly();
                        ret.setId(rs.getLong(1));
                        ret.setName(rs.getString(2));
                        ret.setReadsAssembled(rs.getLong(3));
                        ret.setN50(rs.getInt(4));
                        ret.setAsmjobId(jobId);
                        return Result.ok(ret);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }

        return Result.error("No object of type Assembly for job ID " + jobId + ".");
    }

    private static final String BY_SEQRUN_ID = "SELECT a.id, a.name, a.reads_assembled, a.n50, a.job_id FROM assembly a "
            + "LEFT JOIN job j ON (j.id=a.job_id) "
            + "WHERE ?=ANY(j.seqruns)";

    public Result<AutoCloseableIterator<Assembly>> bySeqRun(long seqrun_id) {
        if (seqrun_id <= 0) {
            return Result.error("No/Invalid ID supplied.");
        }

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(BY_SEQRUN_ID);
            stmt.setLong(1, seqrun_id);
            ResultSet rs = stmt.executeQuery();

            DBIterator<Assembly> dbIterator = new DBIterator<Assembly>(rs, stmt, conn) {
                @Override
                public Assembly convert(ResultSet rs) throws SQLException {
                    Assembly ret = new Assembly();
                    ret.setId(rs.getLong(1));
                    ret.setName(rs.getString(2));
                    ret.setReadsAssembled(rs.getLong(3));
                    ret.setN50(rs.getInt(4));
                    ret.setAsmjobId(rs.getLong(5));
                    return ret;
                }
            };
            return Result.ok(dbIterator);

        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
    }

}
