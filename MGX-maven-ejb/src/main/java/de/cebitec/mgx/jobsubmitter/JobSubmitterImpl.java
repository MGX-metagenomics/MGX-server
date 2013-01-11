package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.DispatcherCommand;
import de.cebitec.mgx.dispatcher.common.JobReceiverI;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import java.io.*;
import java.util.Collection;
import java.util.Properties;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitter {

    @Override
    public boolean verify(final MGXController mgx, long jobId) throws MGXInsufficientJobConfigurationException, MGXException {

        Job job = mgx.getJobDAO().getById(jobId);
        createJobConfigFile(mgx, job);
        

//        JobReceiverI r = getJobReceiver(mgx);
//        if (r != null) {
//            try {
//                r.submit(DispatcherCommand.VERIFY, mgx.getProjectName(), job.getId());
//            } catch (MGXDispatcherException ex) {
//                throw new MGXException(ex.getMessage());
//            }
//            return true;
//        } else {
//            mgx.log("Job verification failed, could not contact dispatcher.");
//            return false;
//        }

//        if (validateParameters(mgx, job)) {
            job.setStatus(JobState.VERIFIED);
            mgx.getJobDAO().update(job);
            return true;
//        }
//
//        return false;
    }

    @Override
    public boolean submit(MGXController mgx, long jobId) throws MGXException, MGXDispatcherException {

        Job job = mgx.getJobDAO().getById(jobId);

        if (job.getStatus() != JobState.VERIFIED) {
            throw new MGXException("Job %s in invalid state %s", job.getId().toString(), job.getStatus());
        }

        job.setStatus(JobState.SUBMITTED);
        mgx.getJobDAO().update(job);

        JobReceiverI r = getJobReceiver(mgx);
        if (r != null) {
            r.submit(DispatcherCommand.EXECUTE, mgx.getProjectName(), job.getId());
            return true;
        } else {
            mgx.log("Job submission failed, could not contact dispatcher.");
            return false;
        }
    }

    @Override
    public boolean cancel(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {

        JobReceiverI r = getJobReceiver(mgx);
        if (r != null) {
            r.submit(DispatcherCommand.CANCEL, mgx.getProjectName(), jobId);
            return true;
        } else {
            mgx.log("Job cancellation failed, could not contact dispatcher.");
            return false;
        }
    }

    @Override
    public void delete(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {

        // notify dispatcher about job delete, so it can be cancelled, 
        // aborted or removed from the queue
        JobReceiverI r = getJobReceiver(mgx);
        if (r != null) {
            r.submit(DispatcherCommand.DELETE, mgx.getProjectName(), jobId);
        } else {
            mgx.log("Job deletion failed, could not contact dispatcher.");
        }
    }

    private JobReceiverI getJobReceiver(MGXController mgx) {
        JobReceiverI r = null;
        try {
            r = (JobReceiverI) getDispatcherContext(mgx).lookup("java:global/MGX-dispatcher-ear/MGX-dispatcher-ejb/JobReceiver");
        } catch (NamingException | MGXDispatcherException ex) {
        }

        return r;
    }

    private Context getDispatcherContext(MGXController mgx) throws NamingException, MGXDispatcherException {

        String dispatcherHost = mgx.getConfiguration().getDispatcherHost();
        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.impl.SerialInitContextFactory");
        props.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
        props.setProperty("java.naming.factory.state", "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");
        props.put("org.omg.CORBA.ORBInitialHost", dispatcherHost);
        props.put("org.omg.CORBA.ORBInitialPort", "3700");

        return new InitialContext(props);
    }

    private void createJobConfigFile(MGXController mgx, Job j) throws MGXException {
        StringBuilder jobconfig = new StringBuilder(mgx.getProjectDirectory())
                .append(File.separator)
                .append("jobs");

        File f = new File(jobconfig.toString());
        f.mkdirs();

        jobconfig.append(File.separator);
        jobconfig.append(j.getId().toString());

        MGXConfiguration mgxcfg = mgx.getConfiguration();

        Collection<JobParameter> params = j.getParameters();

        FileWriter fw = null;
        BufferedWriter cfgFile = null;
        try {
            fw = new FileWriter(jobconfig.toString(), false);
            cfgFile = new BufferedWriter(fw);

            cfgFile.write("mgx.username=" + mgxcfg.getMGXUser());
            cfgFile.newLine();
            cfgFile.write("mgx.password=" + mgxcfg.getMGXPassword());
            cfgFile.newLine();
            cfgFile.write("mgx.host=" + mgx.getDatabaseHost());
            cfgFile.newLine();
            cfgFile.write("mgx.database=" + mgx.getDatabaseName());
            cfgFile.newLine();
            cfgFile.write("mgx.job_id=" + j.getId());
            cfgFile.newLine();

            for (JobParameter jp : params) {
                cfgFile.write(jp.getNodeId() + "." + jp.getParameterName() + "=" + jp.getParameterValue());
                cfgFile.newLine();
            }

        } catch (IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXException(ex.getMessage());
        } finally {
            try {
                cfgFile.close();
                fw.close();
            } catch (IOException ex) {
                mgx.log(ex.getMessage());
                throw new MGXException(ex.getMessage());
            }
        }
    }
}
