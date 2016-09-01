package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sjaenick
 */
public class JobDAO<T extends Job> extends DAO<T> {

    private static final String[] suffices = {"", ".stdout", ".stderr"};

    public JobDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Job.class;
    }

    @Override
    public <T extends Identifiable> T getById(Long id) throws MGXException {
        Job job = super.getById(id);
        fixParameters(job);
        return (T) job;
    }

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<T> getAll() throws MGXException {
        List<T> jobs = getEntityManager().<T>createQuery("SELECT DISTINCT o FROM " + getClassName() + " o", getType())
                .getResultList();
        for (Job j : jobs) {
            fixParameters(j);
        }
        return new ForwardingIterator<>(jobs.iterator());
    }

    @Override
    public void update(T job) throws MGXException {
        super.update(job);
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
                throw new MGXException(ex.getMessage());
            }
        }
    }

    @Override
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

    public AutoCloseableIterator<Job> BySeqRun(SeqRun sr) throws MGXException {
        List<Job> jobs = getEntityManager().<Job>createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun", Job.class).
                setParameter("seqrun", sr).getResultList();
        for (Job j : jobs) {
            fixParameters(j);
        }
        return new ForwardingIterator<>(jobs.iterator());
    }

    public String getError(Job job) throws MGXException {
        if (job.getStatus() != JobState.FAILED) {
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
        String fName = job.getTool().getXMLFile();
        List<JobParameter> availableParams;

        if (paramCache.containsKey(fName)) {
            availableParams = paramCache.get(fName);
        } else {
            String toolXMLData;
            try {
                toolXMLData = UnixHelper.readFile(new File(fName));
            } catch (IOException ex) {
                throw new MGXException(ex);
            }
            availableParams = new ArrayList<>();
            AutoCloseableIterator<JobParameter> apIter = JobParameterHelper.getParameters(toolXMLData, getController().getConfiguration().getPluginDump());
            while (apIter.hasNext()) {
                availableParams.add(apIter.next());
            }
            paramCache.put(fName, availableParams);
        }

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

    @Override
    public void dispose() {
        super.dispose();
        paramCache.clear();
    }
}
