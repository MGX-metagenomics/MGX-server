package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sjaenick
 */
public class JobDAO extends DAO<Job> {

    private static final String[] suffices = {"", ".stdout", ".stderr"};

    public JobDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Job.class;
    }

    private final static String CREATE = "INSERT INTO job (created_by, job_state, seqrun_id, tool_id) "
            + "VALUES (?,?,?,?) RETURNING id";

    @Override
    public long create(Job obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getCreator());
                stmt.setInt(2, obj.getStatus().getValue());
                stmt.setLong(3, obj.getSeqrunId());
                stmt.setLong(4, obj.getToolId());

                try (ResultSet rs = stmt.executeQuery()) {
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
        }
        return obj.getId();
    }

    private final static String BY_ID = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqrun_id, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.id=?";

    @Override
    public Job getById(long id) throws MGXException {

        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        Job job = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
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
                    job.setSeqrunId(rs.getLong(7));
                    job.setParameters(new ArrayList<JobParameter>());

                    //
                    do {
                        if (rs.getLong(8) != 0) {
                            JobParameter jp = new JobParameter();
                            jp.setJobId(job.getId());
                            jp.setId(rs.getLong(9));
                            jp.setNodeId(rs.getLong(10));
                            jp.setParameterName(rs.getString(11));
                            jp.setParameterValue(rs.getString(12));
                            jp.setUserName(rs.getString(13));
                            jp.setUserDescription(rs.getString(14));
                            job.getParameters().add(jp);
                        }
                    } while (rs.next());
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        fixParameters(job);
        return job;
    }

    public AutoCloseableIterator<Job> getByIds(long... ids) throws MGXException {
        if (ids == null || ids.length == 0) {
            throw new MGXException("Null/empty ID list.");
        }
        List<Job> ret = null;

        String BY_IDS = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqrun_id, "
                + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
                + "FROM job j "
                + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id) WHERE j.id IN (" + toSQLTemplateString(ids.length) + ")";

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

//                    while (rs.next()) {
//
//                        if (ret == null) {
//                            ret = new ArrayList<>(ids.size());
//                        }
//
//                        Job job = new Job();
//                        job.setId(rs.getLong(1));
//                        job.setCreator(rs.getString(2));
//                        job.setStatus(JobState.values()[rs.getInt(3)]);
//                        if (rs.getTimestamp(4) != null) {
//                            job.setStartDate(rs.getTimestamp(4));
//                        }
//                        if (rs.getTimestamp(5) != null) {
//                            job.setFinishDate(rs.getTimestamp(5));
//                        }
//                        job.setToolId(rs.getLong(6));
//                        job.setSeqrunId(rs.getLong(7));
//                        job.setParameters(new ArrayList<JobParameter>());
//
//                        //
//                        do {
//                            if (rs.getLong(8) != 0) {
//                                JobParameter jp = new JobParameter();
//                                jp.setJobId(job.getId());
//                                jp.setId(rs.getLong(9));
//                                jp.setNodeId(rs.getLong(10));
//                                jp.setParameterName(rs.getString(11));
//                                jp.setParameterValue(rs.getString(12));
//                                jp.setUserName(rs.getString(13));
//                                jp.setUserDescription(rs.getString(14));
//                                job.getParameters().add(jp);
//                            }
//                        } while (rs.next());
//
//                        ret.add(job);
//                    }
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

    private final static String FETCHALL = "SELECT j.id, j.created_by, j.job_state, j.startdate, j.finishdate, j.tool_id, j.seqrun_id, "
            + "jp.job_id, jp.id, jp.node_id, jp.param_name, jp.param_value, jp.user_name, jp.user_desc "
            + "FROM job j "
            + "LEFT JOIN jobparameter jp ON (j.id=jp.job_id)";

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<Job> getAll() throws MGXException {
        List<Job> ret = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {
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
        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String UPDATE = "UPDATE job SET created_by=?, job_state=?, seqrun_id=?, tool_id=? "
            + "WHERE id=?";

    public void update(Job job) throws MGXException {
        if (job.getId() == Job.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, job.getCreator());
                stmt.setInt(2, job.getStatus().getValue());
                stmt.setLong(3, job.getSeqrunId());
                stmt.setLong(4, job.getToolId());
                stmt.setLong(5, job.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (job.getStatus().equals(JobState.RUNNING)) {
            try (Connection conn = getConnection()) {
                // set start date
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET startdate=NOW() WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                    stmt.close();
                }
            } catch (SQLException ex) {
                throw new MGXException(ex);
            }
        }

        if (job.getStatus().equals(JobState.FINISHED)) {
            try (Connection conn = getConnection()) {
                // set finished date
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET finishdate=NOW() WHERE id=?")) {
                    stmt.setLong(1, job.getId());
                    stmt.executeUpdate();
                    stmt.close();
                }

                // remove attribute counts
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)")) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                    stmt.close();
                }

                // create assignment counts for attributes belonging to this job
                String sql = "INSERT INTO attributecount "
                        + "SELECT attribute.id, count(attribute.id) FROM attribute "
                        + "LEFT JOIN observation ON (attribute.id = observation.attr_id) "
                        + "WHERE job_id=? GROUP BY attribute.id ORDER BY attribute.id";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, job.getId());
                    stmt.execute();
                    stmt.close();
                }
                conn.close();
            } catch (SQLException ex) {
                throw new MGXException(ex);
            }
        }
    }

    public void delete(long id) throws MGXException {
        String sb;
        try {
            sb = new StringBuilder(getController().getProjectJobDirectory().getAbsolutePath())
                    .append(File.separator)
                    .append(id).toString();
        } catch (IOException ex) {
            getController().log(ex.getMessage());
            throw new MGXException(ex);
        }

        for (String suffix : suffices) {
            try {
                Files.deleteIfExists(Paths.get(sb + suffix));
            } catch (IOException ex) {
                getController().log(ex.getMessage());
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
        String fName = tool.getXMLFile();
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
}
