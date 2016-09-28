package de.cebitec.mgx.jobsubmitter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.cebitec.mgx.dispatcher.client.MGXDispatcherConfiguration;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.Host;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.util.UnixHelper;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;
import de.cebitec.mgx.model.db.Job;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitterI {

    @EJB
    MGXDispatcherConfiguration dispConfig;
    //
    private Host currentHost = null;
    private Client currentClient = null;

    private final static String MGX_CLASS = "MGX";

    @Override
    public void shutdown(Host dispatcherHost, String token) throws MGXDispatcherException {
        boolean success = get(dispatcherHost, Boolean.class, "shutdown", token);
        if (!success) {
            throw new MGXDispatcherException("Could not shutdown dispatcher.");
        }
    }

    @Override
    public boolean validate(Host dispatcherHost, String projName, DataSource dataSource, final Job job, String dbHost, String dbName, String dbUser, String dbPass, File projDir) throws MGXDispatcherException {
        if (!createJobConfigFile(dbHost, dbName, dbUser, dbPass, projDir, job.getParameters(), job.getId())) {
            throw new MGXDispatcherException("Failed to write job configuration.");
        }

        boolean ret = get(dispatcherHost, Boolean.class, "validate", MGX_CLASS, projName, String.valueOf(job.getId()));

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET job_state=? WHERE id=?")) {
                stmt.setInt(1, JobState.VERIFIED.getValue());
                stmt.setLong(2, job.getId());
                stmt.executeUpdate();
                job.setStatus(JobState.VERIFIED);
                stmt.close();
            }
        } catch (SQLException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }
        return ret;
    }

    @Override
    public boolean submit(Host dispatcherHost, String projName, DataSource dataSource, Job job) throws MGXDispatcherException {
        if (job.getStatus() != JobState.VERIFIED) {
            throw new MGXDispatcherException("Job %d in invalid state %s", job.getId(), job.getStatus());
        }

        // set job to submitted
        job.setStatus(JobState.SUBMITTED);
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET job_state=? WHERE id=?")) {
                stmt.setInt(1, JobState.SUBMITTED.getValue());
                stmt.setLong(2, job.getId());
                int numRows = stmt.executeUpdate();
                stmt.close();
                if (numRows != 1) {
                    throw new MGXDispatcherException("Could not update job state.");
                }
            }
        } catch (SQLException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }

        // and send to dispatcher
        Boolean ret = get(dispatcherHost, Boolean.class, "submit", MGX_CLASS, projName, String.valueOf(job.getId()));
        return ret;
    }

    @Override
    public void cancel(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "cancel", MGX_CLASS, projectName, String.valueOf(jobId));
    }

    @Override
    public void delete(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "delete", MGX_CLASS, projectName, String.valueOf(jobId));
    }

    private boolean createJobConfigFile(String dbHost, String dbName, String dbUser, String dbPass, File projectDir, Collection<JobParameter> params, long jobId) throws MGXDispatcherException {
        String jobconfigFile = new StringBuilder(projectDir.getAbsolutePath())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(jobId).toString();

        try (BufferedWriter cfgFile = new BufferedWriter(new FileWriter(jobconfigFile, false))) {
            cfgFile.write("mgx.username=" + dbUser);
            cfgFile.newLine();
            cfgFile.write("mgx.password=" + dbPass);
            cfgFile.newLine();
            cfgFile.write("mgx.host=" + dbHost);
            cfgFile.newLine();
            cfgFile.write("mgx.database=" + dbName);
            cfgFile.newLine();
            cfgFile.write("mgx.job_id=" + jobId);
            cfgFile.newLine();
            cfgFile.write("mgx.projectDir=" + projectDir);
            cfgFile.newLine();

            for (JobParameter jp : params) {
                cfgFile.write(jp.getNodeId() + "." + jp.getParameterName() + "=" + jp.getParameterValue());
                cfgFile.newLine();
            }
        } catch (IOException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }

        try {
            UnixHelper.makeFileGroupWritable(jobconfigFile);
        } catch (IOException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }

        return true;
    }

    private WebResource getWebResource(Host target) throws MGXDispatcherException {
        if (target == null) {
            throw new MGXDispatcherException("Invalid null target!");
        }

        if (currentClient == null || (currentHost != null && !currentHost.equals(target))) {
            currentHost = target;
            ClientConfig cc = new DefaultClientConfig();
            cc.getClasses().add(TextPlainReader.class);
            cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 5000); // in ms
            cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 20000); // in ms
            currentClient = Client.create(cc);
        }
        return currentClient.resource(getBaseURI(target));
    }

    private static URI getBaseURI(Host target) throws MGXDispatcherException {
        String uri = new StringBuilder("http://")
                .append(target.getName())
                .append(":4444/MGX-dispatcher-web/webresources/Job/")
                .toString();
        return UriBuilder.fromUri(uri).build();
    }

    protected final <U> U put(Host target, final String path, Object obj, Class<U> c) throws MGXDispatcherException {
        try {
            ClientResponse res = buildPath(target, path).put(ClientResponse.class, obj);
            catchException(res);
            return res.<U>getEntity(c);
        } catch (ClientHandlerException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

    protected final <U> U get(Host target, Class<U> c, final String... path) throws MGXDispatcherException {
        try {
            ClientResponse res = buildPath(target, path).get(ClientResponse.class);
            catchException(res);
            return res.<U>getEntity(c);
        } catch (ClientHandlerException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

    protected final void delete(Host target, final String... path) throws MGXDispatcherException {
        try {
            ClientResponse res = buildPath(target, path).delete(ClientResponse.class);
            catchException(res);
        } catch (ClientHandlerException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

    protected final <U> void post(Host target, U obj, final String... path) throws MGXDispatcherException {
        try {
            ClientResponse res = buildPath(target, path).post(ClientResponse.class, obj);
            catchException(res);
        } catch (ClientHandlerException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

    protected final void catchException(final ClientResponse res) throws MGXDispatcherException {
        if (res == null) {
            throw new MGXDispatcherException("No server response received.");
        }
        if (Status.fromStatusCode(res.getStatus()) != Status.OK) {
            StringBuilder msg = new StringBuilder();
            String buf;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getEntityInputStream()))) {
                while ((buf = r.readLine()) != null) {
                    msg.append(buf);
                    msg.append(System.lineSeparator());
                }
            } catch (IOException ex) {
                Logger.getLogger(JobSubmitterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new MGXDispatcherException(msg.toString());
        }
    }

    private WebResource.Builder buildPath(Host host, String... pathComponents) throws MGXDispatcherException {
        WebResource wr = getWebResource(host);
        try {
            for (String s : pathComponents) {
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }
        return wr.accept(MediaType.TEXT_PLAIN_TYPE);
    }
}
