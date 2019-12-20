package de.cebitec.mgx.web;

import de.cebitec.gpms.core.GPMSMessageI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.dto.ProjectClassDTOFactory;
import de.cebitec.gpms.dto.ProjectDTOFactory;
import de.cebitec.gpms.dto.RoleDTOFactory;
import de.cebitec.gpms.dto.impl.GPMSLong;
import de.cebitec.gpms.dto.impl.GPMSMessageDTO;
import de.cebitec.gpms.dto.impl.GPMSMessageDTOList;
import de.cebitec.gpms.dto.impl.GPMSString;
import de.cebitec.gpms.dto.impl.MembershipDTO;
import de.cebitec.gpms.dto.impl.MembershipDTOList;
import de.cebitec.gpms.dto.impl.ProjectClassDTOList;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.Collection;
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

    @EJB //(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMS")
    DBGPMSI gpms;

    @GET
    @Path("login")
    public GPMSString login() {
        return GPMSString.newBuilder().setValue("MGX").build();
    }

    @GET
    @Path("ping")
    @Produces("application/x-protobuf")
    public GPMSLong ping() {
        return GPMSLong.newBuilder().setValue(System.currentTimeMillis()).build();
    }

    @GET
    @Path("listProjectClasses")
    @Produces("application/x-protobuf")
    public ProjectClassDTOList listProjectClasses() {
        ForwardingIterator<ProjectClassI> forwardingIterator = new ForwardingIterator<>(gpms.getSupportedProjectClasses().iterator());
        return ProjectClassDTOFactory.getInstance().toDTOList(forwardingIterator);
    }

    @GET
    @Path("getMessages")
    @Produces("application/x-protobuf")
    public GPMSMessageDTOList getMessages() {
        GPMSMessageDTOList.Builder ret = GPMSMessageDTOList.newBuilder();
        List<GPMSMessageI> messages = gpms.getMessages();
        for (GPMSMessageI msg : messages) {
            GPMSMessageDTO.Builder b = GPMSMessageDTO.newBuilder();
            b.setDate(msg.getDate().getTime() / 1000L);
            b.setMessage(msg.getText());
            ret.addMessage(b.build());
        }
        return ret.build();
    }

    @GET
    @Path("listMemberships")
    @Produces("application/x-protobuf")
    public MembershipDTOList listMemberships() {
        MembershipDTOList.Builder ret = MembershipDTOList.newBuilder();

        Collection<MembershipI> memberships = gpms.getCurrentUser().getMemberships();
        for (MembershipI m : memberships) {
            MembershipDTO dto = MembershipDTO.newBuilder()
                    .setProject(ProjectDTOFactory.getInstance().toDTO(m.getProject()))
                    .setRole(RoleDTOFactory.getInstance().toDTO(m.getRole()))
                    .build();
            ret.addMembership(dto);
        }
        return ret.build();
    }
}
