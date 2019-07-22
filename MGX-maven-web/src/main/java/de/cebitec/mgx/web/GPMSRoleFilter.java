package de.cebitec.mgx.web;

import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author sj
 */
@Priority(2)
@RequestScoped
public class GPMSRoleFilter implements ContainerRequestFilter {

    @Context
    SecurityContext ctx;
    private DBGPMSI gpms;
    @Context
    private ResourceInfo resourceInfo;

    public GPMSRoleFilter() {
        try {
            gpms = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-gpms-2.0/GPMS");
        } catch (NamingException ex) {
            Logger.getLogger(GPMSRoleFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void filter(ContainerRequestContext cr) {

        if (gpms == null) {
            throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No GPMS access found.");
        }

        UserI user = gpms.getCurrentUser();
        if (user == null) {
            throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No user for this request");
        }

        Method method = resourceInfo.getResourceMethod();
        if (method.getAnnotation(Secure.class) == null) {
            return;
        }

        JDBCMasterI master = gpms.getCurrentMaster();
        if (master == null) {
            throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No master object for this request");
        }

        RoleI role = master.getRole();
        if (role == null) {
            throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No roles assigned for " + user.getLogin());
        }
        ProjectI project = master.getProject();
        if (project == null) {
            throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No project selected");
        }

        //Method method = resourceInfo.getResourceMethod();
        if (method.getAnnotation(Secure.class) != null) {
            Secure roles = method.getAnnotation(Secure.class);
            String[] rightsNeeded = roles.rightsNeeded();
            for (String right : rightsNeeded) {
                if (role.getName().equals(right)) {
                    return; // allowed
                }
            }
        }

        master.log("Denied access to " + cr.getUriInfo().getPath() + " to user " + master.getUser().getLogin() + " in " + project.getName());
        throw new MGXWebException(Response.Status.FORBIDDEN, "Resource access denied.");
    }
}
