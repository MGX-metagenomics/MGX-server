package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.dispatcher.common.MGXInsufficientJobConfigurationException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.cebitec.mgx.dispatcher.client.MGXDispatcherConfiguration;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitter;
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
import javax.sql.DataSource;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitter {

    @EJB
    MGXDispatcherConfiguration dispConfig;

    private Client client = null;
    private String dispatcherHost = null;

    private final static String MGX_CLASS = "MGX/";

    @Override
    public void shutdown(String dispatcherHost, String token) throws MGXDispatcherException {
        boolean success = get(dispatcherHost, "shutdown/" + token, Boolean.class);
        if (!success) {
            throw new MGXDispatcherException("Could not shutdown dispatcher.");
        }
    }

//    @Override
//    public boolean validate(final MGXController mgx, Connection conn, String projName, String dbUser, String dbPass, long jobId) throws MGXInsufficientJobConfigurationException, MGXDispatcherException {
//        Job job;
//        try {
//            job = mgx.getJobDAO().getById(jobId);
//        } catch (MGXException ex) {
//            throw new MGXDispatcherException(ex);
//        }
//        File projectDir;
//        try {
//            projectDir = mgx.getProjectDirectory();
//        } catch (IOException ex) {
//            throw new MGXDispatcherException(ex);
//        }
//        boolean ret = validate(projName, conn, job, dispConfig.getDispatcherHost(), mgx.getDatabaseHost(), mgx.getDatabaseName(), dbUser, dbPass, projectDir);
//        try {
//            conn.close();
//        } catch (SQLException ex) {
//            Logger.getLogger(JobSubmitterImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return ret;
//    }
    @Override
    public boolean validate(String projName, DataSource dataSource, final Job job, String dispatcherHost, String dbHost, String dbName, String dbUser, String dbPass, File projDir) throws MGXInsufficientJobConfigurationException, MGXDispatcherException {
        if (!createJobConfigFile(dbHost, dbName, dbUser, dbPass, projDir, job.getParameters(), job.getId())) {
            throw new MGXDispatcherException("Failed to write job configuration.");
        }

        boolean ret = get(dispatcherHost, "validate/" + MGX_CLASS + projName + "/" + job.getId(), Boolean.class);

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE job SET job_state=? WHERE id=?")) {
                stmt.setInt(1, JobState.VERIFIED.getValue());
                stmt.setLong(2, job.getId());
                stmt.execute();
                stmt.close();
                job.setStatus(JobState.VERIFIED);
            }
        } catch (SQLException ex) {
            throw new MGXDispatcherException(ex.getMessage());
        }
        return ret;
    }

    @Override
    public boolean submit(String dispatcherHost, DataSource dataSource, String projName, Job job) throws MGXDispatcherException {
        if (job.getStatus() != JobState.VERIFIED) {
            throw new MGXDispatcherException("Job %s in invalid state %s", job.getId().toString(), job.getStatus());
        }
//        try {
//            if (conn.isClosed()) {
//                throw new MGXDispatcherException("Cannot submit with closed database connection.");
//            }
//        } catch (SQLException ex) {
//            throw new MGXDispatcherException(ex);
//        }

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
        Boolean ret = get(dispatcherHost, "submit/" + MGX_CLASS + projName + "/" + job.getId(), Boolean.class);
        return ret;
    }

    @Override
    public void cancel(String projectName, long jobId) throws MGXDispatcherException {
        delete("cancel/" + MGX_CLASS + projectName + "/" + jobId);
    }

    @Override
    public void delete(String projectName, long jobId) throws MGXDispatcherException {
        delete("delete/" + MGX_CLASS + projectName + "/" + jobId);
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
        ClientResponse res = getWebResource(dispConfig.getDispatcherHost()).path(path).put(ClientResponse.class, obj);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final <U> U get(String dispatcherHost, final String path, Class<U> c) throws MGXDispatcherException {
        ClientResponse res = getWebResource(dispatcherHost).path(path).get(ClientResponse.class);
        catchException(res);
        return res.<U>getEntity(c);
    }

    protected final void delete(final String path) throws MGXDispatcherException {
        ClientResponse res = getWebResource(dispConfig.getDispatcherHost()).path(path).delete(ClientResponse.class);
        catchException(res);
    }

    protected final <U> void post(final String path, U obj) throws MGXDispatcherException {
        ClientResponse res = getWebResource(dispConfig.getDispatcherHost()).path(path).post(ClientResponse.class, obj);
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
