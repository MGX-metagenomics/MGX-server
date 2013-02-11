package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
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
    public void delete(long id) throws MGXException {
        StringBuilder sb = new StringBuilder(getController().getProjectDirectory()).append(File.separator).append(id);

        boolean all_deleted = true;
        for (String suffix : suffices) {
            File f = new File(sb.toString() + suffix);
            if (f.exists()) {
                boolean deleted = f.delete();
                all_deleted = all_deleted && deleted;
            }
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            // delete observations
            stmt = conn.prepareStatement("DELETE FROM observation WHERE attr_id IN (SELECT id FROM attribute WHERE job_id=?)");
            stmt.setLong(1, id);
            stmt.execute();
            stmt.close();

            // delete attributecounts
            stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id=?)");
            stmt.setLong(1, id);
            stmt.execute(); 
            stmt.close();

            // delete attributes
            stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id=?");
            stmt.setLong(1, id);
            stmt.execute();
            stmt.close();

        } catch (Exception e) {
        } finally {
            close(conn, stmt, null);
        }

        // remove job object
        super.delete(id);
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
        System.out.println("toParameterString: " + parameter);
        return parameter;
    }

    public Iterable<JobParameter> getParameters(String in) {
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
        Iterator iterator = getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.seqrun = :seqrun").
                setParameter("seqrun", sr).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
