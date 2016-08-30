package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
            File f = new File(sb + suffix);
            if (f.exists()) {
                f.delete();
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
        Iterator<Job> iterator = getEntityManager().<Job>createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun", Job.class).
                setParameter("seqrun", sr).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
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
}
