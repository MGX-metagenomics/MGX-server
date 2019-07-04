package de.cebitec.mgx.model.dao;

import com.google.common.hash.Hashing;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.AssemblyJob;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
public class AssemblyJobDAO extends DAO<AssemblyJob> {

    public AssemblyJobDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return AssemblyJob.class;
    }

    private final static String CREATE = "INSERT INTO assemblyjob (seqruns, created_by, apiKey, state) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(AssemblyJob obj) throws MGXException {

        Long[] runids = obj.getSeqrunIds();

        // create a random API key 
        String apiKey = UUID.randomUUID().toString().replaceAll("-", "");
        String sha256hex = Hashing.sha256()
                .hashString(apiKey, StandardCharsets.UTF_8)
                .toString();

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {

                Array array = conn.createArrayOf("BIGINT", runids);
                stmt.setArray(1, array);
                stmt.setString(2, obj.getCreator());
                stmt.setString(3, sha256hex);
                stmt.setInt(4, JobState.CREATED.getValue());

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

    private final static String BY_APIKEY = "SELECT id, seqruns, created_by, startdate, finishdate, state "
            + "FROM assemblyjob WHERE apiKey=?";

    public AssemblyJob getByApiKey(String apiKey) throws MGXException {
        String sha256hex = Hashing.sha256()
                .hashString(apiKey, StandardCharsets.UTF_8)
                .toString();

        AssemblyJob job = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_APIKEY)) {
                stmt.setString(1, sha256hex);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + apiKey + ".");
                    }

                    job = new AssemblyJob();
                    job.setId(rs.getLong(1));
                    Long[] runIds = (Long[]) rs.getArray(2).getArray();
                    job.setSeqruns(runIds);

                    job.setCreator(rs.getString(3));

                    if (rs.getTimestamp(4) != null) {
                        job.setStartDate(rs.getTimestamp(4));
                    }
                    if (rs.getTimestamp(5) != null) {
                        job.setFinishDate(rs.getTimestamp(5));
                    }
                    job.setStatus(JobState.values()[rs.getInt(6)]);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return job;
    }

    private final static String BY_ID = "SELECT id, seqruns, created_by, startdate, finishdate, state "
            + "FROM assemblyjob WHERE id=?";

    @Override
    public AssemblyJob getById(long id) throws MGXException {

        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        AssemblyJob job = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }

                    job = new AssemblyJob();
                    job.setId(rs.getLong(1));
                    Long[] runIds = (Long[]) rs.getArray(2).getArray();
                    job.setSeqruns(runIds);

                    job.setCreator(rs.getString(3));

                    if (rs.getTimestamp(4) != null) {
                        job.setStartDate(rs.getTimestamp(4));
                    }
                    if (rs.getTimestamp(5) != null) {
                        job.setFinishDate(rs.getTimestamp(5));
                    }
                    job.setStatus(JobState.values()[rs.getInt(6)]);
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return job;
    }

    private final static String FETCHALL = "SELECT id, seqruns, created_by, startdate, finishdate, state "
            + "FROM assemblyjob";

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<AssemblyJob> getAll() throws MGXException {
        List<AssemblyJob> ret = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AssemblyJob job = new AssemblyJob();
                        job.setId(rs.getLong(1));
                        Long[] runIds = (Long[]) rs.getArray(2).getArray();
                        job.setSeqruns(runIds);

                        job.setCreator(rs.getString(3));

                        if (rs.getTimestamp(4) != null) {
                            job.setStartDate(rs.getTimestamp(4));
                        }
                        if (rs.getTimestamp(5) != null) {
                            job.setFinishDate(rs.getTimestamp(5));
                        }
                        job.setStatus(JobState.values()[rs.getInt(6)]);

                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        ret.add(job);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String UPDATE = "UPDATE assemblyjob SET seqruns=?, created_by=?, startdate=?, finishdate=?, state=? "
            + "WHERE id=?";

    public void update(AssemblyJob job) throws MGXException {
        if (job.getId() == Job.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {

                Array array = conn.createArrayOf("BIGINT", job.getSeqrunIds());
                stmt.setArray(1, array);
                stmt.setString(2, job.getCreator());
                stmt.setTimestamp(3, new Timestamp(job.getStartDate().getTime()));
                stmt.setTimestamp(4, new Timestamp(job.getFinishDate().getTime()));
                stmt.setInt(5, job.getStatus().getValue());
                stmt.setLong(6, job.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (job.getStatus().equals(JobState.FINISHED)) {
            try (Connection conn = getConnection()) {
                // set finished date
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE assemblyjob SET finishdate=NOW() WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                    stmt.close();
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

    private final static String SQL_BY_SEQRUN = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM seqrun s "
            + "LEFT JOIN job j ON (j.seqrun_id=s.id) "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id)"
            + "WHERE s.id=?";

    public AutoCloseableIterator<Job> bySeqRun(final long run_id) throws MGXException {
        if (run_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<Job> ret = null;

//        SeqRun seqrun = getController().getSeqRunDAO().getById(run_id);
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BY_SEQRUN)) {
                stmt.setLong(1, run_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type SeqRun for ID " + run_id + ".");
                    }

                    Job currentJob = null;

                    do {
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
                                job.setSeqrunId(run_id);
                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(7) != 0 && rs.getLong(7) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(8));
                                jp.setNodeId(rs.getLong(9));
                                jp.setParameterName(rs.getString(10));
                                jp.setParameterValue(rs.getString(11));
                                jp.setUserName(rs.getString(12));
                                jp.setUserDescription(rs.getString(13));

                                currentJob.getParameters().add(jp);
                            }
                        }
                    } while (rs.next());
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

    private final static String BY_ATTRS = "SELECT DISTINCT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqrun_id, "
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
        try (Connection conn = getConnection()) {

            String sql = BY_ATTRS + toSQLTemplateString(attributeIDs.length) + ")";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int pos = 1;
                for (long attrId : attributeIDs) {
                    stmt.setLong(pos++, attrId);
                }
                try (ResultSet rs = stmt.executeQuery()) {

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
                                job.setSeqrunId(rs.getLong(7));
                                job.setParameters(new ArrayList<JobParameter>());

                                if (ret == null) {
                                    ret = new ArrayList<>();
                                }
                                ret.add(job);
                                currentJob = job;
                            }

                            if (rs.getLong(8) != 0 && rs.getLong(8) == currentJob.getId()) {
                                JobParameter jp = new JobParameter();
                                jp.setJobId(currentJob.getId());
                                jp.setId(rs.getLong(9));
                                jp.setNodeId(rs.getLong(10));
                                jp.setParameterName(rs.getString(11));
                                jp.setParameterValue(rs.getString(12));
                                jp.setUserName(rs.getString(13));
                                jp.setUserDescription(rs.getString(14));

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

    public void writeConfigFile(Job job, File projectDir, String dbUser, String dbPass, String dbName, String dbHost) throws MGXException {
        String jobconfigFile = new StringBuilder(projectDir.getAbsolutePath())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(job.getId()).toString();

        try (BufferedWriter cfgFile = new BufferedWriter(new FileWriter(jobconfigFile, false))) {
            cfgFile.write("mgx.username=" + dbUser);
            cfgFile.newLine();
            cfgFile.write("mgx.password=" + dbPass);
            cfgFile.newLine();
            cfgFile.write("mgx.host=" + dbHost);
            cfgFile.newLine();
            cfgFile.write("mgx.database=" + dbName);
            cfgFile.newLine();
            cfgFile.write("mgx.job_id=" + job.getId());
            cfgFile.newLine();
            cfgFile.write("mgx.projectDir=" + projectDir);
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
