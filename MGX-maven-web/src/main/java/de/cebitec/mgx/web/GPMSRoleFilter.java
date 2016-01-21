package de.cebitec.mgx.web;

import com.sun.jersey.api.container.filter.RolesAllowedResourceFilterFactory;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author sj
 */
public class GPMSRoleFilter extends RolesAllowedResourceFilterFactory {

    @Context
    SecurityContext ctx;
    private DBGPMSI gpms;

    public GPMSRoleFilter() {
        lookupGPMS();
    }

    private void lookupGPMS() {
        try {
            gpms = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-gpms/GPMS");
        } catch (NamingException ex) {
            Logger.getLogger(GPMSRoleFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class Filter implements ResourceFilter, ContainerRequestFilter {

        private final DBGPMSI gpmslocal;
        private final String[] rights;

        protected Filter(DBGPMSI gpms, String[] neededRights) {
            this.gpmslocal = gpms;
            this.rights = (neededRights != null) ? neededRights : new String[]{};
        }

        @Override
        public ContainerRequestFilter getRequestFilter() {
            return this;
        }

        @Override
        public ContainerResponseFilter getResponseFilter() {
            return null;
        }

        @Override
        public ContainerRequest filter(ContainerRequest cr) {
            if (rights.length == 0) {
                return cr;
            }
            JPAMasterI master = gpmslocal.getCurrentMaster();
            if (master == null) {
                throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No master object for this request");
            }
            UserI user = gpmslocal.getCurrentUser();
            if (user == null) {
                throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No user for this request");
            }
            RoleI role = master.getRole();
            if (role == null) {
                throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No roles assigned for " + user.getLogin());
            }
            ProjectI project = master.getProject();
            if (project == null) {
                throw new MGXWebException(Response.Status.INTERNAL_SERVER_ERROR, "No project selected");
            }

            for (String right : rights) {
                if (role.getName().equals(right)) {
                    return cr;
                }
            }
            master.log("Denied access to " + cr.getPath() + " to user " + master.getUser().getLogin() + " in " + project.getName());
            throw new MGXWebException(Response.Status.FORBIDDEN, "Resource access denied.");
        }
    }

    /**
     * Retrieves the Secure annotation meta data for the given method or its
     * declaring class.
     *
     * @param method
     * @return An array of needed rights if a
     * @Secure annotation is available, null otherwise.
     */
    private String[] getNeededRights(Method method) {
        if (method.getAnnotation(Secure.class) != null) {
            Secure roles = method.getAnnotation(Secure.class);
            return roles.rightsNeeded();
        }
        return null;
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        String[] neededRights = getNeededRights(am.getMethod());
        return Collections.<ResourceFilter>singletonList(new Filter(gpms, neededRights));
    }
}
