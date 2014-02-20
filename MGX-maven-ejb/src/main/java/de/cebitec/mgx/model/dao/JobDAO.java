package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class JobDAO<T extends Job> extends DAO<T> {

    private static final String[] suffices = {"", ".stdout", ".stderr"};

    @Override
    Class getType() {
        return Job.class;
    }

    @Override
    public void delete(long id) throws MGXException {
        String sb = new StringBuilder(getController().getProjectDirectory())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(id).toString();

        for (String suffix : suffices) {
            File f = new File(sb.toString() + suffix);
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
        String tempString = "";
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

    public AutoCloseableIterator<Job> BySeqRun(SeqRun sr) {
        Iterator<Job> iterator = getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun").
                setParameter("seqrun", sr).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }

    public String getError(Job job) {
        if (job.getStatus() != JobState.FAILED) {
            return "Job is not in FAILED state.";
        }
        String fname = new StringBuilder(getController().getProjectDirectory())
                .append(File.separator).append("jobs")
                .append(File.separator).append(job.getId())
                .append(".stderr").toString();
        StringBuilder ret = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
            String line;
            while ((line = br.readLine()) != null) {
                ret.append(line).append(System.lineSeparator());
            }
        } catch (IOException ex) {
        }
        return ret.toString();
    }
}
