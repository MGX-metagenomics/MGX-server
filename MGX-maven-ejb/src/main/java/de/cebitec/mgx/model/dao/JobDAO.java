package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.JobParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class JobDAO<T extends Job> extends DAO<T> {

    private static String[] suffices = {"", ".xml", ".stdout", ".stderr"};

    @Override
    Class getType() {
        return Job.class;
    }

    @Override
    public void delete(long id) {

        /*
         * here, we only delete the persistent files that might have been
         * created by executing a job. The dispatcher will handle deletion of
         * generated observations and orphan attributes
         */

        StringBuilder sb = new StringBuilder(getController().getProjectDirectory()).append(File.separator).append(id);

        boolean all_deleted = true;
        for (String suffix : suffices) {
            File f = new File(sb.toString() + suffix);
            if (f.exists()) {
                boolean deleted = f.delete();
                all_deleted = all_deleted && deleted;
            }
        }
    }

    public static String toParameterString(Iterable<JobParameter> params) {
        String parameter = "";

        for (JobParameter jobParameter : params) {
            String answer = jobParameter.getConfigItemValue().replaceAll("\n", " ");
            answer = answer.replaceAll("\r\n", " ");

            parameter = parameter + jobParameter.getNodeId() + "."
                    + jobParameter.getConfigItemName() + "\""
                    + answer + "\"";
        }
        return parameter;
    }

    public static Iterable<JobParameter> getParameters(String in) {
        List<JobParameter> ret = new ArrayList<>();
        // muster: nodeid.configname "wert"    
        System.out.println("GetParameters: " + in);
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
                parameter.setConfigItemName(configName);
                parameter.setConfigItemValue(value);
                parameter.setNodeId(nodeId);
                ret.add(parameter);

                if (splitString.length == 3) {
                    nodeId = Long.parseLong(splitString[2]);
                }
            }
        }
        return ret;
    }

    public Iterable<Job> BySeqRun(SeqRun sr) {
        return getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun").
                setParameter("seqrun", sr).getResultList();
    }
}
