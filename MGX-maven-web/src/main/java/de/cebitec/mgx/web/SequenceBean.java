package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
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
import javax.ws.rs.Produces;
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
    @Produces("application/x-protobuf")
    public MGXString init(@PathParam("id") Long seqrun_id) {
        mgx.log("Creating upload session for " + mgx.getProjectName());
        SeqUploadReceiver recv = null;

        try {
            recv = new SeqUploadReceiver(mgx.getConnection(), mgx.getProjectName(), seqrun_id);
//            recv = new SeqUploadReceiver(mgx.getJDBCUrl(), mgx.getProjectName(), seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = sessions.registerUploadSession(recv);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
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
            sessions.getSession(session_id).add(seqList);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }
    
    @GET
    @Path("cancel/{uuid}")
    public Response cancel(@PathParam("uuid") UUID session_id) {
        try {
            sessions.cancelSession(session_id);
         } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }
}
