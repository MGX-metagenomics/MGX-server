package de.cebitec.mgx.jobsubmitter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.util.UnixHelper;
import java.io.*;
import java.net.URI;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitter {

    @Override
    public void shutdown(MGXController mgx) throws MGXDispatcherException {
        String token = mgx.getConfiguration().getDispatcherToken();
        boolean success = get(mgx, "shutdown/" + token, Boolean.class);
        if (!success) {
            throw new MGXDispatcherException("Could not shutdown dispatcher.");
        }
    }

    @Override
    public boolean validate(final MGXController mgx, long jobId) throws MGXInsufficientJobConfigurationException, MGXException {
        Job job = mgx.getJobDAO().getById(jobId);
        createJobConfigFile(mgx, job);
        boolean ret = false;
        try {
            ret = get(mgx, "validate/" + mgx.getProjectName() + "/" + jobId, Boolean.class);
        } catch (MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
        }

        job.setStatus(JobState.VERIFIED);
        mgx.getJobDAO().update(job);
        return ret;
    }

    @Override
    public boolean submit(MGXController mgx, long jobId) throws MGXException, MGXDispatcherException {

        Job job = mgx.getJobDAO().getById(jobId);

        if (job.getStatus() != JobState.VERIFIED) {
            throw new MGXException("Job %s in invalid state %s", job.getId().toString(), job.getStatus());
        }
        boolean ret = false;

        // set job to submitted
        job.setStatus(JobState.SUBMITTED);
        mgx.getJobDAO().update(job);

        // and send to dispatcher
        ret = get(mgx, "submit/" + mgx.getProjectName() + "/" + jobId, Boolean.class);
        return ret;
    }

    @Override
    public void cancel(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {
        delete(mgx, "cancel/" + mgx.getProjectName() + "/" + jobId);
    }

    @Override
    public void delete(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {
        delete(mgx, "delete/" + mgx.getProjectName() + "/" + jobId);
    }

    private boolean createJobConfigFile(MGXController mgx, Job j) throws MGXException {
        StringBuilder jobconfig = new StringBuilder(mgx.getProjectDirectory())
                .append(File.separator)
                .append("jobs");

        File f = new File(jobconfig.toString());
        if (!f.exists()) {
            UnixHelper.createDirectory(f);
        }

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
                if (cfgFile != null) {
                    cfgFile.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException ex) {
                mgx.log(ex.getMessage());
                throw new MGXException(ex.getMessage());
            }
        }
        UnixHelper.makeFileGroupWritable(jobconfig.toString());
        return true;
    }

    private WebResource getWebResource(final MGXController mgx) {
        WebResource service = null;
        try {
            ClientConfig cc = new DefaultClientConfig();
            cc.getClasses().add(TextPlainReader.class);
            Client client = Client.create(cc);
            service = client.resource(getBaseURI(mgx));
        } catch (MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
        }
        return service;
    }

    private URI getBaseURI(final MGXController mgx) throws MGXDispatcherException {
        String uri = new StringBuilder("http://")
                .append(mgx.getConfiguration().getDispatcherHost())
                .append(":4444/MGX-dispatcher-web/webresources/Job/")
                .toString();
        return UriBuilder.fromUri(uri).build();
    }

    protected final <U> U put(MGXController mgx, final String path, Object obj, Class<U> c) throws MGXDispatcherException {
        //System.err.println("PUT uri: " + getWebResource().path(path).getURI().toASCIIString());
        ClientResponse res = getWebResource(mgx).path(path).put(ClientResponse.class, obj);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final <U> U get(MGXController mgx, final String path, Class<U> c) throws MGXDispatcherException {
        //System.err.println("GET uri: " +getWebResource().path(path).getURI().toASCIIString());
        ClientResponse res = getWebResource(mgx).path(path).get(ClientResponse.class);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final void delete(MGXController mgx, final String path) throws MGXDispatcherException {
        //System.err.println("DELETE uri: " +getWebResource().path(path).getURI().toASCIIString());
        ClientResponse res = getWebResource(mgx).path(path).delete(ClientResponse.class);
        catchException(res);
    }

    protected final <U> void post(MGXController mgx, final String path, U obj) throws MGXDispatcherException {
        ClientResponse res = getWebResource(mgx).path(path).post(ClientResponse.class, obj);
        catchException(res);
    }

    protected final void catchException(final ClientResponse res) throws MGXDispatcherException {
        if (res.getClientResponseStatus() != Status.OK) {
            InputStreamReader isr = new InputStreamReader(res.getEntityInputStream());
            BufferedReader r = new BufferedReader(isr);
            StringBuilder msg = new StringBuilder();
            String buf;
            try {
                while ((buf = r.readLine()) != null) {
                    msg.append(buf);
                }
                r.close();
                isr.close();
            } catch (IOException ex) {
                Logger.getLogger(JobSubmitterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new MGXDispatcherException(msg.toString());
        }
    }
}
