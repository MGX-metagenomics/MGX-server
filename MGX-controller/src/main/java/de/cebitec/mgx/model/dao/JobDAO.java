package de.cebitec.mgx.model.dao;

import com.google.common.hash.Hashing;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dispatcher.common.api.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.workers.DeleteJob;
import de.cebitec.mgx.workers.RestartJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
public class JobDAO extends DAO<Job> {

    public JobDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Job> getType() {
        return Job.class;
    }

    private final static String CREATE = "INSERT INTO job (created_by, job_state, seqruns, assembly, tool_id) "
            + "VALUES (?,?,?,?,?) RETURNING id";

    @Override
    public long create(Job obj) throws MGXException {

        if ((obj.getSeqrunIds() == null || obj.getSeqrunIds().length == 0) && obj.getAssemblyId() == Identifiable.INVALID_IDENTIFIER) {
            throw new MGXException("Job object does not reference sequencing runs or assemblies.");
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getCreator());
                stmt.setInt(2, obj.getStatus().getValue());

                Long[] temp = null;
                if (obj.getSeqrunIds() != null) {
                    temp = new Long[obj.getSeqrunIds().length];
                    for (int i = 0; i < temp.length; i++) {
                        temp[i] = obj.getSeqrunIds()[i];
                    }

                    Array array = conn.createArrayOf("BIGINT", temp);
                    stmt.setArray(3, array);
                    stmt.setNull(4, Types.BIGINT);
                } else {
                    stmt.setNull(3, Types.ARRAY);
                    stmt.setLong(4, obj.getAssemblyId());
                }

                stmt.setLong(5, obj.getToolId());

                try ( ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (obj.getParameters() != null && !obj.getParameters().isEmpty()) {
            for (JobParameter jp : obj.getParameters()) {
                jp.setJobId(obj.getId());
            }

            //
            // persist parameters
            //
            getController().getJobParameterDAO().create(obj.getParameters().toArray(new JobParameter[]{}));
        }

        return obj.getId();
    }

    public TaskI delete(long id) throws IOException {
        return new DeleteJob(id, getController().getDataSource(), getController().getProjectName(),
                getController().getProjectJobDirectory().getAbsolutePath());
    }

    public TaskI restart(Job job, String dispHost, GPMSManagedDataSourceI ds, String projectName, JobSubmitterI js) throws IOException, MGXDispatcherException {
        return new RestartJob(dispHost, job, ds, projectName, js);
    }

    @Override
    public Job getById(long id) throws MGXException {
        return getById(id, true);
    }

    private final static String BY_ID = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.id=?";

    public Job getById(long id, boolean mangleParameters) throws MGXException {

        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        Job job = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Job for ID " + id + ".");
                    }

                    job = new Job();
                    job.setId(rs.getLong(1));
                    job.setCreator(rs.getString(2));
                    job.setStatus(JobState.values()[rs.getInt(3)]);
                    if (rs.getTimestamp(4) != null) {
                        job.setStartDate(rs.getTimestamp(4));
                    }
                    if (rs.getTimestamp(5) != null) {
                        job.setFinishDate(rs.getTimestamp(5));
                    }
                    job.setToolId(rs.getLong(6));

                    Array array = rs.getArray(7);
                    if (array != null) {
                        Long[] values = (Long[]) array.getArray();
                        long[] tmp = new long[values.length];
                        for (int i = 0; i < tmp.length; i++) {
                            tmp[i] = values[i];
                        }
                        job.setSeqrunIds(tmp);
                    } else {
                        job.setAssemblyId(rs.getLong(8));
                    }

                    job.setParameters(new ArrayList<JobParameter>());

                    //
                    do {
                        if (rs.getLong(9) != 0) {
                            JobParameter jp = new JobParameter();
                            jp.setJobId(job.getId());
                            jp.setId(rs.getLong(10));
                            jp.setNodeId(rs.getLong(11));
                            jp.setParameterName(rs.getString(12));
                            jp.setParameterValue(rs.getString(13));
                            jp.setUserName(rs.getString(14));
                            jp.setUserDescription(rs.getString(15));
                            job.getParameters().add(jp);
                        }
                    } while (rs.next());
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (mangleParameters) {
            fixParameters(job);
        }
        return job;
    }

    public AutoCloseableIterator<Job> getByIds(long... ids) throws MGXException {
        if (ids == null || ids.length == 0) {
            throw new MGXException("Null/empty ID list.");
        }
        List<Job> ret = null;

        String BY_IDS = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
                + "j.assembly, "
                + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
                + "FROM job j "
                + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.id IN (" + toSQLTemplateString(ids.length) + ")";

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_IDS)) {
                int idx = 1;
                for (long id : ids) {
                    if (id <= 0) {
                        throw new MGXException("No/Invalid ID supplied.");
                    }
                    stmt.setLong(idx++, id);
                }

                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) rs.getArray(7).getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    job.setAssemblyId(rs.getLong(8));
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String BY_APIKEY = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.apikey=?";

    public Job getByApiKey(String apiKey) throws MGXException {

        String sha256hex = Hashing.sha256()
                .hashString(apiKey, StandardCharsets.UTF_8)
                .toString();

        // DEBUG
        //System.err.println("API key " + apiKey + " hashed to " + sha256hex);

        Job job = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BY_APIKEY)) {
                stmt.setString(1, sha256hex);
                try ( ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("Invalid API key.");
                    }

                    job = new Job();
                    job.setId(rs.getLong(1));
                    job.setCreator(rs.getString(2));
                    job.setStatus(JobState.values()[rs.getInt(3)]);
                    if (rs.getTimestamp(4) != null) {
                        job.setStartDate(rs.getTimestamp(4));
                    }
                    if (rs.getTimestamp(5) != null) {
                        job.setFinishDate(rs.getTimestamp(5));
                    }
                    job.setToolId(rs.getLong(6));
                    Array array = rs.getArray(7);
                    if (array != null) {
                        Long[] values = (Long[]) array.getArray();
                        long[] tmp = new long[values.length];
                        for (int i = 0; i < tmp.length; i++) {
                            tmp[i] = values[i];
                        }
                        job.setSeqrunIds(tmp);
                    } else {
                        long assemblyId = rs.getLong(8);
                        job.setAssemblyId(assemblyId);
                    }
                    job.setParameters(new ArrayList<JobParameter>());

                    //
                    do {
                        if (rs.getLong(9) != 0) {
                            JobParameter jp = new JobParameter();
                            jp.setJobId(job.getId());
                            jp.setId(rs.getLong(10));
                            jp.setNodeId(rs.getLong(11));
                            jp.setParameterName(rs.getString(12));
                            jp.setParameterValue(rs.getString(13));
                            jp.setUserName(rs.getString(14));
                            jp.setUserDescription(rs.getString(15));
                            job.getParameters().add(jp);
                        }
                    } while (rs.next());
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return job;
    }

    private final static String FETCHALL = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id)";

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<Job> getAll() throws MGXException {
        List<Job> ret = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) rs.getArray(7).getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    long assemblyId = rs.getLong(8);
                                    job.setAssemblyId(assemblyId);
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String BYTOOL = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.tool_id=?";

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<Job> byTool(long tool_id) throws MGXException {
        List<Job> ret = null;
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(BYTOOL)) {
                stmt.setLong(1, tool_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) array.getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    job.setAssemblyId(rs.getLong(8));
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String CREATE_APIKEY = "UPDATE job SET apikey=? WHERE id=?";

    public String createApiKey(Job obj) throws MGXException {
        // create a random API key 
        String apiKey = UUID.randomUUID().toString().replaceAll("-", "");
        String sha256hex = Hashing.sha256()
                .hashString(apiKey, StandardCharsets.UTF_8)
                .toString();
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(CREATE_APIKEY)) {
                stmt.setString(1, sha256hex);
                stmt.setLong(2, obj.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return apiKey;
    }

    private final static String UPDATE = "UPDATE job SET created_by=?, job_state=?, seqruns=?, assembly=?, tool_id=? "
            + "WHERE id=?";

    public void update(Job job) throws MGXException {
        if (job.getId() == Job.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type Job without an ID.");
        }

        if ((job.getSeqrunIds() == null || job.getSeqrunIds().length == 0) && job.getAssemblyId() == Identifiable.INVALID_IDENTIFIER) {
            throw new MGXException("Job object does not reference sequencing runs or assemblies.");
        }

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, job.getCreator());
                stmt.setInt(2, job.getStatus().getValue());
                if (job.getSeqrunIds() != null) {
                    Long[] temp = new Long[job.getSeqrunIds().length];
                    for (int i = 0; i < temp.length; i++) {
                        temp[i] = job.getSeqrunIds()[i];
                    }
                    Array array = conn.createArrayOf("BIGINT", temp);
                    stmt.setArray(3, array);
                    stmt.setNull(4, Types.BIGINT);
                } else {
                    stmt.setNull(3, Types.ARRAY);
                    stmt.setLong(4, job.getAssemblyId());
                }

                stmt.setLong(5, job.getToolId());
                stmt.setLong(6, job.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (job.getStatus().equals(JobState.RUNNING) || job.getStatus().equals(JobState.SUBMITTED)) {
            try ( Connection conn = getConnection()) {
                // set start date
                try ( PreparedStatement stmt = conn.prepareStatement("UPDATE job SET startdate=NOW() WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                }
            } catch (SQLException ex) {
                throw new MGXException(ex);
            }
        }

        if (job.getStatus().equals(JobState.FINISHED)) {
            try ( Connection conn = getConnection()) {
                // set finished date
                try ( PreparedStatement stmt = conn.prepareStatement("UPDATE job SET finishdate=NOW() WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                }

                // remove attribute counts
                try ( PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }

                // create assignment counts for read-based attributes belonging to this job
                String sql = "INSERT INTO attributecount "
                        + "SELECT attribute.id, read.seqrun_id, count(attribute.id) FROM attribute "
                        + "LEFT JOIN observation ON (attribute.id = observation.attr_id) "
                        + "LEFT JOIN read ON (observation.seq_id=read.id) "
                        + "WHERE job_id=? GROUP BY attribute.id, read.seqrun_id ORDER BY attribute.id";

                try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }

                // create coverage-based counts for gene attributes
                String sql2 = "INSERT INTO attributecount "
                        + "SELECT attribute.id, gene_coverage.run_id, sum(gene_coverage.coverage) FROM attribute "
                        + "LEFT JOIN gene_observation ON (attribute.id = gene_observation.attr_id) "
                        + "LEFT JOIN gene ON (gene_observation.gene_id=gene.id) "
                        + "LEFT JOIN gene_coverage ON (gene.id=gene_coverage.gene_id) "
                        + "WHERE job_id=? AND gene_coverage.coverage > 0 "
                        + "GROUP BY attribute.id, gene_coverage.run_id ORDER BY attribute.id";

                try ( PreparedStatement stmt = conn.prepareStatement(sql2)) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                }

                // invalidate api key
                try ( PreparedStatement stmt = conn.prepareStatement("UPDATE job SET apikey=NULL WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                }

            } catch (SQLException ex) {
                throw new MGXException(ex);
            }
        }
    }

    public static String toParameterString(Iterable<JobParameter> params) {
        String parameter = "";

        for (JobParameter jobParameter : params) {
            String answer = jobParameter.getParameterValue().replaceAll("\n", " ");
            answer = answer.replaceAll("\r\n", " ");

            parameter = parameter + jobParameter.getNodeId() + "."
                    + jobParameter.getParameterName() + "\""
                    + answer + "\"";
        }
        return parameter;
    }

    public Iterable<JobParameter> getParameters(String in) {
        List<JobParameter> ret = new ArrayList<>(5);
        // muster: nodeid.configname "wert"    
        String[] tempSplit = in.split("\\.");
        JobParameter parameter;
        String tempString;
        long nodeId = 0;

        for (int i = 0; i < tempSplit.length; i++) {

            tempString = tempSplit[i];

            if (i == 0) {
                nodeId = Long.parseLong(tempString);
            } else {
                tempString = tempSplit[i];
                String[] splitString = tempString.split("\"");
                String configName = splitString[0];
                String value = splitString[1];

                parameter = new JobParameter();
                parameter.setParameterName(configName);
                parameter.setParameterValue(value);
                parameter.setNodeId(nodeId);
                ret.add(parameter);

                if (splitString.length == 3) {
                    nodeId = Long.parseLong(splitString[2]);
                }
            }
        }
        return ret;
    }

    private final static String SQL_BY_SEQRUN = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) "
            + "WHERE ?=ANY(j.seqruns)";

    public AutoCloseableIterator<Job> bySeqRun(final long run_id) throws MGXException {
        if (run_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<Job> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BY_SEQRUN)) {
                stmt.setLong(1, run_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) rs.getArray(7).getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    job.setAssemblyId(rs.getLong(8));
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String SQL_BY_ASM = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) "
            + "WHERE j.assembly=?";

    public AutoCloseableIterator<Job> byAssembly(final long asm_id) throws MGXException {
        if (asm_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<Job> ret = null;

        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement(SQL_BY_ASM)) {
                stmt.setLong(1, asm_id);
                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) rs.getArray(7).getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    job.setAssemblyId(rs.getLong(8));
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String BY_ATTRS = "SELECT DISTINCT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqruns, "
            + "j.assembly, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM attribute a "
            + "LEFT JOIN job j ON (a.job_id=j.id)"
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) "
            + "WHERE a.id IN (";

    public List<Job> byAttributes(long[] attributeIDs) throws MGXException {
        if (attributeIDs == null || attributeIDs.length == 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        List<Job> ret = null;
        try ( Connection conn = getConnection()) {

            String sql = BY_ATTRS + toSQLTemplateString(attributeIDs.length) + ")";

            try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
                int pos = 1;
                for (long attrId : attributeIDs) {
                    stmt.setLong(pos++, attrId);
                }
                try ( ResultSet rs = stmt.executeQuery()) {

                    Job currentJob = null;

                    while (rs.next()) {
                        if (rs.getLong(1) != 0) {
                            if (currentJob == null || rs.getLong(1) != currentJob.getId()) {
                                Job job = new Job();
                                job.setId(rs.getLong(1));
                                job.setCreator(rs.getString(2));
                                job.setStatus(JobState.values()[rs.getInt(3)]);
                                if (rs.getTimestamp(4) != null) {
                                    job.setStartDate(rs.getTimestamp(4));
                                }
                                if (rs.getTimestamp(5) != null) {
                                    job.setFinishDate(rs.getTimestamp(5));
                                }
                                job.setToolId(rs.getLong(6));

                                Array array = rs.getArray(7);
                                if (array != null) {
                                    Long[] values = (Long[]) rs.getArray(7).getArray();
                                    long[] tmp = new long[values.length];
                                    for (int i = 0; i < tmp.length; i++) {
                                        tmp[i] = values[i];
                                    }
                                    job.setSeqrunIds(tmp);
                                } else {
                                    job.setAssemblyId(rs.getLong(8));
                                }

                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(9) != 0 && rs.getLong(9) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(10));
                                jp.setNodeId(rs.getLong(11));
                                jp.setParameterName(rs.getString(12));
                                jp.setParameterValue(rs.getString(13));
                                jp.setUserName(rs.getString(14));
                                jp.setUserDescription(rs.getString(15));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        if (ret != null) {
            for (Job j : ret) {
                fixParameters(j);
            }
        }
        return ret;
    }

    public String getError(Job job) throws MGXException {
        if (job.getStatus() != JobState.FAILED) {
            //getController().log("state: "+job.getStatus());
            return "Job is not in FAILED state.";
        }
        try {
            String fname = new StringBuilder(getController().getProjectJobDirectory().getAbsolutePath())
                    .append(File.separator).append(job.getId())
                    .append(".stderr").toString();
            return UnixHelper.readFile(new File(fname));
        } catch (IOException ex) {
            getController().log(ex.getMessage());
        }
        return "";
    }

    private final Map<String, List<JobParameter>> paramCache = new HashMap<>();

    private void fixParameters(Job job) throws MGXException {
        Tool tool = getController().getToolDAO().getById(job.getToolId());
        String fName = tool.getFile();
        if (!tool.getFile().endsWith(".xml")) {
            return;
        }
        List<JobParameter> availableParams;

        if (paramCache.containsKey(fName)) {
            availableParams = paramCache.get(fName);
        } else {
            String toolXMLData;
            try {
                toolXMLData = UnixHelper.readFile(new File(fName));
            } catch (IOException ex) {
                getController().log(ex);
                throw new MGXException(ex);
            }
            availableParams = new ArrayList<>();
            AutoCloseableIterator<JobParameter> apIter = JobParameterHelper.getParameters(toolXMLData, getController().getPluginDump());
            while (apIter.hasNext()) {
                availableParams.add(apIter.next());
            }
            paramCache.put(fName, availableParams);
        }

        if (job.getParameters() != null && !job.getParameters().isEmpty()) {

            final String projectFileDir;
            try {
                projectFileDir = getController().getProjectFileDirectory().getAbsolutePath();
            } catch (IOException ex) {
                throw new MGXException(ex);
            }

            for (JobParameter jp : job.getParameters()) {
                for (JobParameter candidate : availableParams) {
                    // can't compare by ID field here
                    if (jp.getNodeId() == candidate.getNodeId() && jp.getParameterName().equals(candidate.getParameterName())) {
                        jp.setClassName(candidate.getClassName());
                        jp.setType(candidate.getType());
                        jp.setDisplayName(candidate.getDisplayName());
                    }
                }

                // do not expose internal path names
                if (jp.getParameterValue() != null && jp.getParameterValue().startsWith(projectFileDir + File.separator)) {
                    jp.setParameterValue(jp.getParameterValue().replaceAll(projectFileDir + File.separator, ""));
                }
                if (jp.getParameterValue() != null && jp.getParameterValue().startsWith(projectFileDir)) {
                    jp.setParameterValue(jp.getParameterValue().replaceAll(projectFileDir, ""));
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        paramCache.clear();
    }

    public void writeCWLConfigFile(Job job, File projectDir, String projectName, URI annotationService) throws MGXException {

        String jobconfigFile = new StringBuilder(projectDir.getAbsolutePath())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(job.getId())
                .toString();

        try ( BufferedWriter cfgFile = new BufferedWriter(new FileWriter(jobconfigFile, false))) {
            cfgFile.write("projectName: " + projectName);
            cfgFile.newLine();
            cfgFile.write("apiKey: " + createApiKey(job));
            cfgFile.newLine();
            cfgFile.write("hostURI: " + annotationService.toASCIIString());
            cfgFile.newLine();
            cfgFile.write("runIds:");
            cfgFile.newLine();
            for (long runId : job.getSeqrunIds()) {
                cfgFile.write("  - \"");
                cfgFile.write(String.valueOf(runId));
                cfgFile.write("\"");
                cfgFile.newLine();
            }

            for (JobParameter jp : job.getParameters()) {
                cfgFile.write(jp.getParameterName() + ": ");
                if (jp.getClassName().equals("string")) {
                    cfgFile.write("\"" + jp.getParameterValue() + "\"");
                } else if (jp.getClassName().equals("int")) {
                    cfgFile.write(jp.getParameterValue());
                } else {
                    throw new MGXException("Unknown parameter type: " + jp.getClassName());
                }
                cfgFile.newLine();
            }
            cfgFile.flush();
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }

        try {
            UnixHelper.makeFileGroupWritable(jobconfigFile);
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    public void writeConveyorConfigFile(Job job, URI annotationService, String projectName, File projectDir, String dbUser, String dbPass, String dbName, String dbHost, int dbPort) throws MGXException {

        String jobconfigFile = new StringBuilder(projectDir.getAbsolutePath())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(job.getId()).toString();

        try ( BufferedWriter cfgFile = new BufferedWriter(new FileWriter(jobconfigFile, false))) {
            cfgFile.write("mgx.username=" + dbUser);
            cfgFile.newLine();
            cfgFile.write("mgx.password=" + dbPass);
            cfgFile.newLine();
            cfgFile.write("mgx.host=" + dbHost);
            cfgFile.newLine();
            cfgFile.write("mgx.database=" + dbName);
            cfgFile.newLine();
            cfgFile.write("mgx.port=" + dbPort);
            cfgFile.newLine();
            cfgFile.write("mgx.job_id=" + job.getId());
            cfgFile.newLine();
            cfgFile.write("mgx.projectDir=" + projectDir);
            cfgFile.newLine();
            cfgFile.write("mgx.apiKey=" + createApiKey(job));
            cfgFile.newLine();
            cfgFile.write("mgx.annotationService=" + annotationService.toASCIIString());
            cfgFile.newLine();
            cfgFile.write("mgx.projectName=" + projectName);
            cfgFile.newLine();

            for (JobParameter jp : job.getParameters()) {
                cfgFile.write(jp.getNodeId() + "." + jp.getParameterName() + "=" + jp.getParameterValue());
                cfgFile.newLine();
            }
            cfgFile.flush();
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }

        try {
            UnixHelper.makeFileGroupWritable(jobconfigFile);
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

}
