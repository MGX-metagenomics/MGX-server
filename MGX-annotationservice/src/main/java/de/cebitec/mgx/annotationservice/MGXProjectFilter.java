package de.cebitec.mgx.annotationservice;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.mgx.controller.MGXRoles;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Provider
@PreMatching
@Priority(1)
@RequestScoped
public class MGXProjectFilter implements ContainerRequestFilter {

    @Context
    SecurityContext ctx;
    private DBGPMSI gpms;

    public MGXProjectFilter() {
        lookupGPMS();
    }

    private void lookupGPMS() {
        try {
            gpms = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-gpms-2.0/GPMS");
        } catch (NamingException ex) {
            Logger.getLogger(MGXProjectFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void filter(ContainerRequestContext cr) {

        String path = cr.getUriInfo().getPath();
        String resource = path;
        String projectName = "";
        int idx = path.indexOf('/', 0);
        if (idx != -1) {
            projectName = path.substring(0, idx);
            resource = path.substring(idx + 1);
        }
        
        //Logger.getLogger(MGXProjectFilter.class.getName()).log(Level.SEVERE, "PATH: {0}", path);

        if (projectName == null || projectName.isEmpty()) {
            throw new WebApplicationException("No project name provided.", Response.Status.UNAUTHORIZED);
        }

        UriBuilder ub = cr.getUriInfo().getBaseUriBuilder();
        ub.replacePath(cr.getUriInfo().getBaseUri().getPath() + resource);
        cr.setRequestUri(cr.getUriInfo().getBaseUri(), ub.build());

        MembershipI serviceAccess = null;
        try {
            serviceAccess = gpms.getService(projectName, MGXRoles.User);
            if (serviceAccess == null) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } catch (GPMSException ex) {
            Logger.getLogger(MGXProjectFilter.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        gpms.createServiceMaster(serviceAccess, JDBCMasterI.class);
    }
}
