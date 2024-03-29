package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.dispatcher.common.api.MGXDispatcherException;
import de.cebitec.mgx.jobsubmitter.api.Host;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.concurrent.TimeUnit;
import org.glassfish.jersey.client.ClientConfig;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobSubmitter")
public class JobSubmitterImpl implements JobSubmitterI {

    private Host currentHost = null;
    private Client currentClient = null;
    //
    // GPMS project class for MGX 2.x projects
    private final static String MGX_PROJECT_CLASS = "MGX-2";
    //
    public final static String URI_BASE = "/MGX-dispatcher-web/webresources/Job/";
    public final static int DEFAULT_DISPATCHER_PORT = 4444;


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
        return get(dispatcherHost, Boolean.class, "validate", MGX_PROJECT_CLASS, projName, String.valueOf(jobId));
    }

    @Override
    public boolean submit(Host dispatcherHost, String projName, long jobId) throws MGXDispatcherException {
        return get(dispatcherHost, Boolean.class, "submit", MGX_PROJECT_CLASS, projName, String.valueOf(jobId));
    }

    @Override
    public void cancel(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "cancel", MGX_PROJECT_CLASS, projectName, String.valueOf(jobId));
    }

    @Override
    public void delete(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException {
        delete(dispatcherHost, "delete", MGX_PROJECT_CLASS, projectName, String.valueOf(jobId));
    }

    private WebTarget getWebResource(Host target) throws MGXDispatcherException {
        if (target == null) {
            throw new MGXDispatcherException("Invalid null target!");
        }

        if (currentClient == null || (currentHost != null && !currentHost.equals(target))) {
            currentHost = target;
            ClientConfig cc = new ClientConfig();
            cc.register(TextPlainReader.class);

            currentClient = ClientBuilder.newBuilder()
                    .withConfig(cc)
                    .connectTimeout(5_000, TimeUnit.MILLISECONDS)
                    .readTimeout(30_000, TimeUnit.MILLISECONDS)
                    .build();
        }
        return currentClient.target(getBaseURI(target));
    }

    private static URI getBaseURI(Host target) throws MGXDispatcherException {
        String uri = new StringBuilder("http://")
                .append(target.getName())
                .append(":4444/MGX-dispatcher-web/webresources/Job/")
                .toString();
        return UriBuilder.fromUri(uri).build();
    }

//    protected final <U> U put(Host target, final String path, Object obj, Class<U> targetClass) throws MGXDispatcherException {
//        Invocation.Builder buildPath = buildPath(target, path);
//        try (Response res = buildPath.put(Entity.entity(obj, MediaType.TEXT_PLAIN_TYPE))) {
//            catchException(res);
//            return res.readEntity(targetClass);
//        } catch (ProcessingException che) {
//            if (che.getCause() != null && che.getCause() instanceof Exception) {
//                throw new MGXDispatcherException(che.getCause().getMessage());
//            } else {
//                throw new MGXDispatcherException(che.getMessage());
//            }
//        }
//    }

    protected final <U> U get(Host target, Class<U> targetClass, final String... path) throws MGXDispatcherException {
        Invocation.Builder buildPath = buildPath(target, path);
        try (Response res = buildPath.get(Response.class)) {
            catchException(res);
            return res.readEntity(targetClass);
        } catch (ProcessingException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

    protected final void delete(Host target, final String... path) throws MGXDispatcherException {
        Invocation.Builder buildPath = buildPath(target, path);
        try {
            Response res = buildPath.delete(Response.class);
            catchException(res);
        } catch (ProcessingException che) {
            if (che.getCause() != null && che.getCause() instanceof Exception) {
                throw new MGXDispatcherException(che.getCause().getMessage());
            } else {
                throw new MGXDispatcherException(che.getMessage());
            }
        }
    }

//    protected final <U> void post(Host target, U obj, final String... path) throws MGXDispatcherException {
//        Invocation.Builder buildPath = buildPath(target, path);
//        try {
//            Response res = buildPath.post(Entity.entity(obj, MediaType.TEXT_PLAIN_TYPE));
//            catchException(res);
//        } catch (ProcessingException che) {
//            if (che.getCause() != null && che.getCause() instanceof Exception) {
//                throw new MGXDispatcherException(che.getCause().getMessage());
//            } else {
//                throw new MGXDispatcherException(che.getMessage());
//            }
//        }
//    }

    protected final void catchException(final Response res) throws MGXDispatcherException {
        if (Response.Status.fromStatusCode(res.getStatus()) != Response.Status.OK) {
            StringBuilder msg = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.readEntity(InputStream.class)))) {
                String buf;
                while ((buf = r.readLine()) != null) {
                    msg.append(buf);
                    msg.append(System.lineSeparator());
                }
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
            throw new MGXDispatcherException(msg.toString().trim());
        }
    }

    private Invocation.Builder buildPath(Host host, String... pathComponents) throws MGXDispatcherException {
        WebTarget wr = getWebResource(host);
        try {
            for (String s : pathComponents) {
                wr = wr.path(URLEncoder.encode(s, "UTF-8"));
            }
            //System.err.println(wr.getURI().toASCIIString());

            return wr.request(MediaType.TEXT_PLAIN_TYPE).accept(MediaType.TEXT_PLAIN_TYPE);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @PreDestroy
    public void dispose() {
        if (currentClient != null) {
            currentClient.close();
        }
    }
}
