package de.cebitec.mgx.web;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.MembershipDTO;
import de.cebitec.mgx.dto.dto.MembershipDTOList;
import de.cebitec.mgx.dto.dto.MembershipDTOList.Builder;
import de.cebitec.mgx.dto.dto.ProjectClassDTOList;
import de.cebitec.mgx.dtoadapter.ProjectClassDTOFactory;
import de.cebitec.mgx.dtoadapter.ProjectDTOFactory;
import de.cebitec.mgx.dtoadapter.RoleDTOFactory;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("GPMSBean")
public class ProjectBean {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMS")
    DBGPMSI gpms;

    @GET
    @Path("login")
    public MGXString login() {
        return MGXString.newBuilder().setValue("MGX").build();
    }

    @GET
    @Path("ping")
    public MGXLong ping() {
        return MGXLong.newBuilder().setValue(System.currentTimeMillis()).build();
    }

    @GET
    @Path("listProjectClasses")
    @Produces("application/x-protobuf")
    public ProjectClassDTOList listProjectClasses() {
        ForwardingIterator<ProjectClassI> forwardingIterator = new ForwardingIterator<>(gpms.getSupportedProjectClasses().iterator());
        return ProjectClassDTOFactory.getInstance().toDTOList(forwardingIterator);
    }

    @GET
    @Path("listMemberships")
    @Produces("application/x-protobuf")
    public MembershipDTOList listMemberships() {
        Builder ret = MembershipDTOList.newBuilder();

        for (ProjectClassI pc : gpms.getSupportedProjectClasses()) {
            if ("MGX".equals(pc.getName())) {
                List<? extends MembershipI> memberships = gpms.getCurrentUser().getMemberships(pc);
                for (MembershipI m : memberships) {
                    MembershipDTO dto = MembershipDTO.newBuilder()
                            .setProject(ProjectDTOFactory.getInstance().toDTO(m.getProject()))
                            .setRole(RoleDTOFactory.getInstance().toDTO(m.getRole()))
                            .build();
                    ret.addMembership(dto);
                }
            }
        }
        return ret.build();
    }
}
