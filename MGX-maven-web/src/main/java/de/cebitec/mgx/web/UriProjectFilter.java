package de.cebitec.mgx.web;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.DBMembershipI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

public class UriProjectFilter implements ContainerRequestFilter {

    @Context
    SecurityContext ctx;
    private DBGPMSI gpms;

    public UriProjectFilter() {
        lookupGPMS();
    }

    private void lookupGPMS() {
        try {
            gpms = InitialContext.doLookup("java:global/MGX-maven-ear/MGX-gpms/GPMS");
        } catch (NamingException ex) {
            Logger.getLogger(UriProjectFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getProjectFromPath(String path) {
        int idx = path.indexOf('/');
        if (idx != -1) {
            return path.substring(0, idx);
        } else {
            return "";
        }
    }

    private String removeProjectFromPath(String path) {
        int idx = path.indexOf('/');
        if (idx != -1) {
            return path.substring(idx + 1);
        } else {
            return path;
        }
    }

    @Override
    public ContainerRequest filter(ContainerRequest cr) {
        String project = getProjectFromPath(cr.getPath());
        String resource = removeProjectFromPath(cr.getPath());

        UriBuilder ub = cr.getRequestUriBuilder();
        ub.replacePath(cr.getBaseUri().getPath() + resource);
        cr.setUris(cr.getBaseUri(), ub.build());

        if ("GPMS".equals(project)) {
            return cr;
        }

        UserI u = gpms.getCurrentUser();
        for (ProjectClassI pc : gpms.getSupportedProjectClasses()) {
            for (MembershipI m : u.getMemberships(pc)) {
                if (m.getProject().getName().equals(project)) {
                    // will create a new master or re-use a cached one
                    gpms.createMaster((DBMembershipI)m);
                }
            }
        }
        return cr;
    }
}
