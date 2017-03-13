package de.cebitec.mgx.jobsubmitter;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.cebitec.mgx.dispatcher.common.api.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.Host;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitterI {

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
    public boolean validate(Host dispatcherHost, String projName, long jobId) throws MGXDispatcherException {
        //
        // dispatcher will update the DB job state to either JobState.VERIFIED
        // or JobState.FAILED
        //
        return get(dispatcherHost, Boolean.class, "validate", MGX_CLASS, projName, String.valueOf(jobId));
    }

    @Override
    public boolean submit(Host dispatcherHost, String projName, long jobId) throws MGXDispatcherException {
        return get(dispatcherHost, Boolean.class, "submit", MGX_CLASS, projName, String.valueOf(jobId));
    }

    @Override
    public void cancel(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "cancel", MGX_CLASS, projectName, String.valueOf(jobId));
    }

    @Override
    public void delete(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "delete", MGX_CLASS, projectName, String.valueOf(jobId));
    }

    private WebResource getWebResource(Host target) throws MGXDispatcherException {
        if (target == null) {
            throw new MGXDispatcherException("Invalid null target!");
        }

        if (currentClient == null || (currentHost != null && !currentHost.equals(target))) {
            currentHost = target;
            ClientConfig cc = new DefaultClientConfig();
            cc.getClasses().add(TextPlainReader.class);
            cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 5_000); // in ms
            cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 30_000); // in ms
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
