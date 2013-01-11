package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.SeqByAttributeDownloadProvider;
import de.cebitec.mgx.download.SeqRunDownloadProvider;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.upload.SeqUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
    UploadSessions upSessions;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-web/DownloadSessions")
    DownloadSessions downSessions;

    /*
     * 
     * Upload interface
     * 
     * 
     */
    @GET
    @Path("initUpload/{id}")
    @Produces("application/x-protobuf")
    public MGXString initUpload(@PathParam("id") Long seqrun_id) {
        mgx.log("Creating upload session for " + mgx.getProjectName());
        SeqUploadReceiver recv = null;

        try {
            recv = new SeqUploadReceiver(mgx.getConnection(), mgx.getProjectName(), seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = upSessions.registerUploadSession(recv);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeUpload/{uuid}")
    public Response closeUpload(@PathParam("uuid") UUID session_id) {
        try {
            upSessions.closeSession(session_id);
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
            upSessions.getSession(session_id).add(seqList);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("cancelUpload/{uuid}")
    public Response cancelUpload(@PathParam("uuid") UUID session_id) {
        try {
            upSessions.cancelSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    /*
     * 
     * Download interface
     * 
     * 
     */
    @GET
    @Path("initDownload/{id}")
    @Produces("application/x-protobuf")
    public MGXString initDownload(@PathParam("id") Long seqrun_id) {
        SeqRunDownloadProvider provider = null;
        try {
            // make sure requested run exists
            mgx.getSeqRunDAO().getById(seqrun_id);
            mgx.log("Creating download session for " + mgx.getProjectName());
            provider = new SeqRunDownloadProvider(mgx.getConnection(), mgx.getProjectName(), seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @POST
    @Path("initDownloadforAttributes/")
    @Produces("application/x-protobuf")
    public MGXString initDownloadforAttributes(AttributeDTOList attrdtos) {
        SeqByAttributeDownloadProvider provider = null;
        try {
            Collection<Long> ids = new ArrayList<>();
            for (AttributeDTO dto : attrdtos.getAttributeList()) {
                ids.add(dto.getId());
            }
            Set<Attribute> attrs = new HashSet<>();
            Iterator<Attribute> iter = mgx.getAttributeDAO().getByIds(ids);
            while (iter.hasNext()) {
                attrs.add(iter.next());
            }
            
            mgx.log("Creating download session for " + mgx.getProjectName());
            provider = new SeqByAttributeDownloadProvider(mgx.getConnection(), mgx.getProjectName(), attrs);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
            downSessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetchSequences/{uuid}")
    @Consumes("application/x-protobuf")
    public SequenceDTOList fetchSequences(@PathParam("uuid") UUID session_id) {
        try {
            DownloadProviderI<SequenceDTOList> session = downSessions.getSession(session_id);
            return session.fetch();
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
    }

    @GET
    @Path("cancelDownload/{uuid}")
    public Response cancelDownload(@PathParam("uuid") UUID session_id) {
        try {
            downSessions.cancelSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    /*
     * 
     * retrieval of single sequences
     * 
     */
    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public SequenceDTO fetch(@PathParam("id") Long id) {
        Sequence obj;
        try {
            obj = mgx.getSequenceDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SequenceDTOFactory.getInstance().toDTO(obj);
    }
}
