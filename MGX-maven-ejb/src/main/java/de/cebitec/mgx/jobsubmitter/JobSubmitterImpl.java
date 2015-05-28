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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitter {

    @EJB  // (lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;

    private Client client = null;
    private String dispatcherHost = null;

    private final static String MGX_CLASS = "MGX/";

    @Override
    public void shutdown(MGXController mgx) throws MGXDispatcherException {
        String token = mgxconfig.getDispatcherToken();
        boolean success = get(mgxconfig.getDispatcherHost(), "shutdown/" + token, Boolean.class);
        if (!success) {
            throw new MGXDispatcherException("Could not shutdown dispatcher.");
        }
    }

    @Override
    public boolean validate(final MGXController mgx, long jobId) throws MGXInsufficientJobConfigurationException, MGXException {
        Job job = mgx.getJobDAO().getById(jobId);
        Connection conn = mgx.getConnection();
        File projectDir;
        try {
            projectDir = mgx.getProjectDirectory();
        } catch (IOException ex) {
            throw new MGXException(ex);
        }
        boolean ret = validate(mgx.getProjectName(), conn, job, mgxconfig, mgx.getDatabaseHost(), mgx.getDatabaseName(), projectDir);
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(JobSubmitterImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    @Override
    public boolean validate(String projName, Connection conn, final Job job, MGXConfiguration config, String dbHost, String dbName, File projDir) throws MGXInsufficientJobConfigurationException, MGXException {
        if (!createJobConfigFile(config, dbHost, dbName, projDir, job)) {
            throw new MGXException("Failed to write job configuration.");
        }
        boolean ret = false;
        try {
            ret = get(config.getDispatcherHost(), "validate/" + MGX_CLASS + projName + "/" + job.getId(), Boolean.class);
        } catch (MGXDispatcherException ex) {
            throw new MGXException(ex.getMessage());
        }

//        try {
//            if (conn.isClosed()) {
//                throw new MGXException("Cannot validate with closed database connection.");
//            }
//        } catch (SQLException ex) {
//            throw new MGXException(ex);
//        }

        try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET job_state=? WHERE id=?")) {
            stmt.setInt(1, JobState.VERIFIED.getValue());
            stmt.setLong(2, job.getId());
            stmt.execute();
            stmt.close();
            job.setStatus(JobState.VERIFIED);
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        return ret;
    }

    @Override
    public boolean submit(String dispatcherHost, Connection conn, String projName, Job job) throws MGXException, MGXDispatcherException {
        if (job.getStatus() != JobState.VERIFIED) {
            throw new MGXException("Job %s in invalid state %s", job.getId().toString(), job.getStatus());
        }
        try {
            if (conn.isClosed()) {
                throw new MGXException("Cannot submit with closed database connection.");
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        // set job to submitted
        job.setStatus(JobState.SUBMITTED);
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET job_state=? WHERE id=?")) {
            stmt.setInt(1, JobState.SUBMITTED.getValue());
            stmt.setLong(2, job.getId());
            int numRows = stmt.executeUpdate();
            stmt.close();
            if (numRows != 1) {
                throw new MGXException("Could not update job state.");
            }
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        }
        // and send to dispatcher
        Boolean ret = get(dispatcherHost, "submit/" + MGX_CLASS + projName + "/" + job.getId(), Boolean.class);
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(JobSubmitterImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    @Override
    public void cancel(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {
        delete("cancel/" + MGX_CLASS + mgx.getProjectName() + "/" + jobId);
    }

    @Override
    public void delete(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException {
        delete("delete/" + MGX_CLASS + mgx.getProjectName() + "/" + jobId);
    }

    private boolean createJobConfigFile(MGXConfiguration mgxcfg, String dbHost, String dbName, File projectDir, Job j) throws MGXException {
        String jobconfigFile = new StringBuilder(projectDir.getAbsolutePath())
                .append(File.separator)
                .append("jobs")
                .append(File.separator)
                .append(j.getId().toString()).toString();

        Collection<JobParameter> params = j.getParameters();

        try (BufferedWriter cfgFile = new BufferedWriter(new FileWriter(jobconfigFile, false))) {
            cfgFile.write("mgx.username=" + mgxcfg.getMGXUser());
            cfgFile.newLine();
            cfgFile.write("mgx.password=" + mgxcfg.getMGXPassword());
            cfgFile.newLine();
            cfgFile.write("mgx.host=" + dbHost);
            cfgFile.newLine();
            cfgFile.write("mgx.database=" + dbName);
            cfgFile.newLine();
            cfgFile.write("mgx.job_id=" + j.getId());
            cfgFile.newLine();
            cfgFile.write("mgx.projectDir=" + projectDir);
            cfgFile.newLine();

            for (JobParameter jp : params) {
                cfgFile.write(jp.getNodeId() + "." + jp.getParameterName() + "=" + jp.getParameterValue());
                cfgFile.newLine();
            }
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
        
        try {
            UnixHelper.makeFileGroupWritable(jobconfigFile);
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
        
        return true;
    }

    private WebResource getWebResource(final String dispHost) {
        if (dispatcherHost != null && dispatcherHost.equals(dispHost)) {
            return client.resource(getBaseURI());
        } else {
            dispatcherHost = dispHost;
            ClientConfig cc = new DefaultClientConfig();
            cc.getClasses().add(TextPlainReader.class);
            client = Client.create(cc);
            return client.resource(getBaseURI());
        }
    }

    private URI getBaseURI() {
        String uri = new StringBuilder("http://")
                .append(dispatcherHost)
                .append(":4444/MGX-dispatcher-web/webresources/Job/")
                .toString();
        return UriBuilder.fromUri(uri).build();
    }

    protected final <U> U put(final String path, Object obj, Class<U> c) throws MGXDispatcherException {
        ClientResponse res = getWebResource(mgxconfig.getDispatcherHost()).path(path).put(ClientResponse.class, obj);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final <U> U get(String dispatcherHost, final String path, Class<U> c) throws MGXDispatcherException {
        ClientResponse res = getWebResource(dispatcherHost).path(path).get(ClientResponse.class);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final void delete(final String path) throws MGXDispatcherException {
        ClientResponse res = getWebResource(mgxconfig.getDispatcherHost()).path(path).delete(ClientResponse.class);
        catchException(res);
    }

    protected final <U> void post(final String path, U obj) throws MGXDispatcherException {
        ClientResponse res = getWebResource(mgxconfig.getDispatcherHost()).path(path).post(ClientResponse.class, obj);
        catchException(res);
    }

    protected final void catchException(final ClientResponse res) throws MGXDispatcherException {
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
}
