package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ReferenceDTO;
import de.cebitec.mgx.dto.dto.ReferenceDTOList;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTOList;
import de.cebitec.mgx.dtoadapter.ReferenceDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.worker.InstallGlobalReference;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.upload.ReferenceUploadReceiver;
import de.cebitec.mgx.upload.UploadReceiverI;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Result<Reference> obj = mgx.getReferenceDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return ReferenceDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        Result<TaskI> delete;
        try {
            delete = mgx.getReferenceDAO().delete(id);
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        if (delete.isError()) {
            throw new MGXWebException(delete.getError());
        }
        UUID taskId = taskHolder.addTask(delete.getValue());
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ReferenceDTOList fetchall() {
        Result<AutoCloseableIterator<Reference>> all = mgx.getReferenceDAO().getAll();
        if (all.isError()) {
            throw new MGXWebException(all.getError());
        }
        return ReferenceDTOFactory.getInstance().toDTOList(all.getValue());
    }

    @GET
    @Path("listGlobalReferences")
    @Produces("application/x-protobuf")
    public ReferenceDTOList listGlobalReferences() {
        Result<AutoCloseableIterator<Reference>> res = global.getReferenceDAO().getAll();
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }
        return ReferenceDTOFactory.getInstance().toDTOList(res.getValue());
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
    @Path("getSequence/{refid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public MGXString getSequence(@PathParam("refid") Long id, @PathParam("from") int from, @PathParam("to") int to) {
        Result<String> subseq = mgx.getReferenceDAO().getSequence(id, from, to);
        if (subseq.isError()) {
            throw new MGXWebException(subseq.getError());
        }
        return MGXString.newBuilder().setValue(subseq.getValue()).build();
    }

    @GET
    @Path("init/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString init(@PathParam("id") Long ref_id) {

        Result<Reference> obj = mgx.getReferenceDAO().getById(ref_id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }

        Reference ref = obj.getValue();
        mgx.log("creating reference importer session for " + ref.getName());

        UUID uuid = null;
        try {
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
            ReferenceUploadReceiver recv = (ReferenceUploadReceiver) upSessions.<ReferenceRegionDTOList>getSession(session_id);
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
    public Response addRegions(@PathParam("uuid") UUID session_id, ReferenceRegionDTOList dtos) {
        try {
            UploadReceiverI<ReferenceRegionDTOList> recv = upSessions.getSession(session_id);
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
