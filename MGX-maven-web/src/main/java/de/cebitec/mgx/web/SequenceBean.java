package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.SequenceDTOList;
import de.cebitec.mgx.upload.SeqUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Sequence")
public class SequenceBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-web/UploadSessions")
    UploadSessions sessions;

    @GET
    @Path("init/{id}")
    public String init(@PathParam("id") Long seqrun_id) {
        mgx.log("Creating upload session for " + mgx.getProjectName());
        SeqUploadReceiver recv = null;

        try {
            recv = new SeqUploadReceiver(mgx.getJDBCUrl(), mgx.getProjectName(), seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        return sessions.registerUploadSession(recv).toString();
    }

    @GET
    @Path("close/{uuid}")
    public Response close(@PathParam("uuid") UUID session_id) {
        try {
            sessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @POST
    @Path("add/{uuid}")
    @Consumes("application/x-protobuf")
    public Response add(@PathParam("uuid") UUID session_id, SequenceDTOList seqList) {
        try {
            sessions.addData(session_id, seqList);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }
}
