package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ReferenceDTO;
import de.cebitec.mgx.dto.dto.ReferenceDTOList;
import de.cebitec.mgx.dto.dto.RegionDTOList;
import de.cebitec.mgx.dtoadapter.ReferenceDTOFactory;
import de.cebitec.mgx.dtoadapter.RegionDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.MGXGlobalException;
import de.cebitec.mgx.global.worker.InstallGlobalReference;
import de.cebitec.mgx.workers.DeleteReference;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.upload.ReferenceUploadReceiver;
import de.cebitec.mgx.upload.UploadReceiverI;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author belmann
 */
@Stateless
@Path("Reference")
public class ReferenceBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;
    @EJB
    UploadSessions upSessions;
    @EJB
    MappingSessions mappingSessions;
    @EJB
    MGXGlobal global;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(ReferenceDTO dto) {
        long Reference_id;
        try {
            Reference ref = ReferenceDTOFactory.getInstance().toDB(dto);
            Reference_id = mgx.getReferenceDAO().create(ref);
        } catch (MGXException ex) {
            mgx.log(ex);
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(Reference_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(ReferenceDTO dto) {

        Reference reference = ReferenceDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getReferenceDAO().update(reference);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public ReferenceDTO fetch(@PathParam("id") Long id) {
        Reference obj = null;
        try {
            obj = mgx.getReferenceDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ReferenceDTOFactory.getInstance().toDTO(obj);
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId;
        try {
            mgx.getReferenceDAO().getById(id);
            taskId = taskHolder.addTask(new DeleteReference(mgx.getDataSource(), id, mgx.getProjectName(), mappingSessions));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ReferenceDTOList fetchall() {
        try {
            return ReferenceDTOFactory.getInstance().toDTOList(mgx.getReferenceDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("listGlobalReferences")
    @Produces("application/x-protobuf")
    public ReferenceDTOList listGlobalReferences() {
        try {
            return ReferenceDTOFactory.getInstance().toDTOList(global.getReferenceDAO().getAll());
        } catch (MGXGlobalException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("installGlobalReference/{refid}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString installGlobalReference(@PathParam("refid") Long globalId) {
        String projReferenceDir;
        try {
            projReferenceDir = mgx.getProjectReferencesDirectory().getAbsolutePath();
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        
        UUID taskId = taskHolder.addTask(new InstallGlobalReference(mgx.getDataSource(), global, globalId, projReferenceDir, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("byReferenceInterval/{refid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public RegionDTOList byReferenceInterval(@PathParam("refid") Long refid, @PathParam("from") int from, @PathParam("to") int to) {
        RegionDTOList ret = null;
        try {
            Reference ref = mgx.getReferenceDAO().getById(refid);
            ret = RegionDTOFactory.getInstance().toDTOList(mgx.getReferenceDAO().byReferenceInterval(refid, ref, from, to));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ret;
    }

    @GET
    @Path("getSequence/{refid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public MGXString getSequence(@PathParam("refid") Long id, @PathParam("from") int from, @PathParam("to") int to) {
        String subseq = null;
        try {
            subseq = mgx.getReferenceDAO().getSequence(id, from, to);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(subseq).build();
    }

    @GET
    @Path("init/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString init(@PathParam("id") Long ref_id) {
        Reference ref = null;
        UUID uuid = null;
        try {
            ref = mgx.getReferenceDAO().getById(ref_id);
            mgx.log("creating reference importer session for " + ref.getName());
            File target = new File(mgx.getProjectReferencesDirectory(), ref.getId() + ".fas");
            ref.setFile(target.getAbsolutePath());
            ReferenceUploadReceiver recv = new ReferenceUploadReceiver(ref, mgx.getProjectName(), mgx.getDataSource());
            uuid = upSessions.registerUploadSession(recv);
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @PUT
    @Path("addSequence/{uuid}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response addSequence(@PathParam("uuid") UUID session_id, MGXString chunkDNA) {
        try {
            ReferenceUploadReceiver recv = (ReferenceUploadReceiver) upSessions.<RegionDTOList>getSession(session_id);
            recv.addSequenceData(chunkDNA.getValue().toUpperCase());
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @PUT
    @Path("addRegions/{uuid}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response addRegions(@PathParam("uuid") UUID session_id, RegionDTOList dtos) {
        try {
            UploadReceiverI<RegionDTOList> recv = upSessions.getSession(session_id);
            recv.add(dtos);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("close/{uuid}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response close(@PathParam("uuid") UUID session_id) {
        mgx.log(mgx.getCurrentUser() + " closing reference importer session for " + mgx.getProjectName());
        try {
            upSessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }
}
