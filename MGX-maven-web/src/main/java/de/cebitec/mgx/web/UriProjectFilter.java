package de.cebitec.mgx.web;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JDBCMasterI;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;


@PreMatching
@Priority(1)
@RequestScoped
public class UriProjectFilter implements ContainerRequestFilter {
    
    // FIXME currently annotated as @RequestScoped due to 
    // https://github.com/payara/Payara/issues/3994

    @Context
    SecurityContext ctx;
    private DBGPMSI gpms;

    public UriProjectFilter() {
        lookupGPMS();
    }

    private void lookupGPMS() {
        try {
            gpms = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-gpms-2.0/GPMS");
        } catch (NamingException ex) {
            Logger.getLogger(UriProjectFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //
    // WARNING: setting DEBUG_AUTH to true will log cleartext passwords
    //
    private final static boolean DEBUG_AUTH = false;
    private static final Set<String> cache = new HashSet<>();

    @Override
    public void filter(ContainerRequestContext cr) {

        // Get the authentication passed in HTTP headers parameters
        if (DEBUG_AUTH) {
            String auth = cr.getHeaderString("authorization");
            if (auth != null) {
                int idx = auth.indexOf(" ");
                if (idx != -1) {
                    auth = auth.substring(++idx);
                }
                //auth = auth.replaceFirst("[Bb]asic ", "");
                String userColonPass = String.valueOf(Base64.getDecoder().decode(auth));
                if (!cache.contains(userColonPass)) {
                    Logger.getLogger(UriProjectFilter.class.getName()).log(Level.INFO, "DEBUG: {0}", userColonPass);
                    cache.add(userColonPass);
                }
            }
        }

        String path = cr.getUriInfo().getPath();
        String resource = path;
        String projectName = "";
        int idx = path.indexOf('/', 0);
        if (idx != -1) {
            projectName = path.substring(0, idx);
            resource = path.substring(idx + 1);
        }

        UriBuilder ub = cr.getUriInfo().getBaseUriBuilder();
        ub.replacePath(cr.getUriInfo().getBaseUri().getPath() + resource);
        cr.setRequestUri(cr.getUriInfo().getBaseUri(), ub.build());

        
        if ("GPMS".equals(projectName)) {
            // pass-through
            return;
        }

        UserI u = gpms.getCurrentUser();
        for (MembershipI m : u.getMemberships()) {
            if (m.getProject().getName().equals(projectName)) {
                // will create a new master or re-use a cached one
                gpms.createMaster(m, JDBCMasterI.class);
            }
        }
    }
}
