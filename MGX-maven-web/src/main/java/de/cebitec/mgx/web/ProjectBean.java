package de.cebitec.mgx.web;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.util.ProjectClassFactory;
import de.cebitec.mgx.dto.MembershipDTO;
import de.cebitec.mgx.dto.MembershipDTOList;
import de.cebitec.mgx.dto.MembershipDTOList.Builder;
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
@Path("Project")
public class ProjectBean {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMS")
    DBGPMSI gpms;

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public MembershipDTOList fetchall() {
        Builder ret = MembershipDTOList.newBuilder();

        List<? extends MembershipI> memberships = gpms.getCurrentUser().getMemberships(ProjectClassFactory.getProjectClass(gpms, "MGX"));
        for (MembershipI m : memberships) {
            MembershipDTO dto = MembershipDTO.newBuilder()
                    .setProject(m.getProject().getName())
                    .setRole(m.getRole().getName())
                    .build();
            ret.addMembership(dto);
        }
        return ret.build();
    }
}
