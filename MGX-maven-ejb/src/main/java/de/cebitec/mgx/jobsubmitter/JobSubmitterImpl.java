package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.DispatcherCommand;
import de.cebitec.mgx.dispatcher.common.JobReceiverI;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
    public boolean verify(MGXController mgx, Long jobId) throws MGXInsufficientJobConfigurationException, MGXException {

        Job job = mgx.getJobDAO().getById(jobId);

        if (job.getStatus() != JobState.CREATED) {
            throw new MGXException("Job %s in invalid state %s", job.getId().toString(), job.getStatus());
        }

        if (validateParameters(mgx, job)) {
            job.setStatus(JobState.VERIFIED);
            mgx.getJobDAO().update(job);
            return true;
        }

        return false;
    }

    @Override
    public boolean submit(MGXController mgx, Long jobId) throws MGXException, MGXDispatcherException {

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
    public boolean cancel(MGXController mgx, Long jobId) throws MGXDispatcherException, MGXException {
        Job job = mgx.getJobDAO().getById(jobId);

        if (job == null) {
            throw new MGXException("No such job");
        }

        JobReceiverI r = getJobReceiver(mgx);
        if (r != null) {
            r.submit(DispatcherCommand.CANCEL, mgx.getProjectName(), job.getId());
            return true;
        } else {
            mgx.log("Job cancellation failed, could not contact dispatcher.");
            return false;
        }
    }
    
    @Override
    public void delete(MGXController mgx, Long jobId) throws MGXDispatcherException, MGXException {
        Job job = mgx.getJobDAO().getById(jobId);

        if (job == null) {
            throw new MGXException("No such job");
        }

        JobReceiverI r = getJobReceiver(mgx);
        if (r != null) {
            r.submit(DispatcherCommand.CANCEL, mgx.getProjectName(), job.getId());
        } else {
            mgx.log("Job deletion failed, could not contact dispatcher.");
        }
    }

    private JobReceiverI getJobReceiver(MGXController mgx) {
        JobReceiverI r = null;
        try {
            r = (JobReceiverI) getDispatcherContext(mgx).lookup("java:global/MGX-dispatcher-ear/MGX-dispatcher-ejb/JobReceiver");
        } catch (Exception ex) {
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

    private boolean validateParameters(MGXController mgx, Job j) throws MGXInsufficientJobConfigurationException, MGXException {

        // build up command string
        List<String> commands = new ArrayList<String>();
        commands.add(mgx.getConfiguration().getValidatorExecutable());
        commands.add(j.getTool().getXMLFile());
        commands.add(createJobConfigFile(mgx, j));

        StringBuilder cmd = new StringBuilder();
        for (String s : commands) {
            cmd.append(s);
            cmd.append(" ");
        }
        
        String[] argv = commands.toArray(new String[0]);

        StringBuilder output = new StringBuilder();
        Integer exitCode = null;
        try {
            Process p = Runtime.getRuntime().exec(argv);
            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = null;
            while ((s = stdout.readLine()) != null) {
                output.append(s);
            }
            stdout.close();

            while (exitCode == null) {
                try {
                    exitCode = p.waitFor();
                } catch (InterruptedException ex) {
                }
            }
        } catch (IOException ex) {
            mgx.log(ex.getMessage());
        }

        if (exitCode.intValue() == 0) {
            return true;
        }

        throw new MGXInsufficientJobConfigurationException(output.toString());
    }

    private String createJobConfigFile(MGXController mgx, Job j) throws MGXException {
        StringBuilder jobconfig = new StringBuilder(mgx.getProjectDirectory());
        jobconfig.append(File.separator);
        jobconfig.append("jobs");

        File f = new File(jobconfig.toString());
        f.mkdirs();

        jobconfig.append(File.separator);
        jobconfig.append(j.getId());

        MGXConfiguration mgxcfg = mgx.getConfiguration();

        String[] params = j.getParameters().split("\\s+");

        FileWriter fw = null;
        try {
            fw = new FileWriter(jobconfig.toString(), false);
            BufferedWriter cfgFile = new BufferedWriter(fw);

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

            for (String s : params) {
                cfgFile.write(s);
                cfgFile.newLine();
            }

            cfgFile.close();
            fw.close();
        } catch (IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXException(ex.getMessage());
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                mgx.log(ex.getMessage());
                throw new MGXException(ex.getMessage());
            }
        }

        return jobconfig.toString();
    }
}
