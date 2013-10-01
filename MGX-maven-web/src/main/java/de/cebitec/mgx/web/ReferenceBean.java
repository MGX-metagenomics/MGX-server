package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ReferenceDTOList;
import de.cebitec.mgx.dto.dto.RegionDTOList;
import de.cebitec.mgx.dtoadapter.ReferenceDTOFactory;
import de.cebitec.mgx.dtoadapter.RegionDTOFactory;
import de.cebitec.mgx.model.dao.deleteworkers.DeleteReference;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public dto.MGXLong create(dto.ReferenceDTO dto) {
        Long Reference_id = null;
        try {
            //Sample s = mgx.getSampleDAO().getById(dto.getSampleId());
            Reference ref = ReferenceDTOFactory.getInstance().toDB(dto);

            Reference_id = mgx.getReferenceDAO().create(ref);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(Reference_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    public Response update(dto.ReferenceDTO dto) {

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
    public dto.ReferenceDTO fetch(@PathParam("id") Long id) {
        Reference obj = null;
        try {
            obj = (Reference) mgx.getReferenceDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ReferenceDTOFactory.getInstance().toDTO(obj);
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId = taskHolder.addTask(new DeleteReference(mgx.getConnection(), id, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public dto.ReferenceDTOList fetchall() {
        return ReferenceDTOFactory.getInstance().toDTOList(mgx.getReferenceDAO().getAll());
    }

    @GET
    @Path("listGlobalReferences")
    @Produces("application/x-protobuf")
    public ReferenceDTOList listGlobalReferences() {
        return ReferenceDTOFactory.getInstance().toDTOList(mgx.getGlobal().getReferenceDAO().getAll());
    }

    @GET
    @Path("installGlobalReference/{refid}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong installGlobalReference(@PathParam("refid") Long globalId) {
        File referencesDir = new File(mgx.getProjectDirectory() + "/reference/");
        if (!referencesDir.exists()) {
            UnixHelper.createDirectory(referencesDir);
        }

        Reference globalRef = null;
        try {
            globalRef = mgx.getGlobal().getReferenceDAO().getById(globalId);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        Reference newRef = new Reference();
        newRef.setFile("");
        newRef.setName(globalRef.getName());
        newRef.setFile(globalRef.getFile());
        newRef.setLength(globalRef.getLength());
        newRef.setRegions(new ArrayList<Region>());

        for (Region r : globalRef.getRegions()) {
            Region newReg = new Region();
            newReg.setDescription(r.getDescription());
            newReg.setReference(newRef);
            newReg.setStart(r.getStart());
            newReg.setStop(r.getStop());
            newRef.getRegions().add(newReg);
        }

        try {
            mgx.getReferenceDAO().create(newRef);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        File targetFile = new File(mgx.getProjectDirectory() + "/reference/" + newRef.getId() + ".fas");
        try {
            UnixHelper.copyFile(new File(globalRef.getFile()), targetFile);
        } catch (IOException ex) {
            mgx.log(ex.getMessage());
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw new MGXWebException(ex.getMessage());
        }

        newRef.setFile(targetFile.getAbsolutePath());
        try {
            mgx.getReferenceDAO().update(newRef);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }


        return MGXLong.newBuilder().setValue(newRef.getId()).build();
    }

    @GET
    @Path("byReferenceInterval/{refid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public RegionDTOList byReferenceInterval(@PathParam("refid") Long id, @PathParam("from") int from, @PathParam("from") int to) {
        Reference ref;
        try {
            ref = mgx.getReferenceDAO().getById(id);
            return RegionDTOFactory.getInstance().toDTOList(mgx.getReferenceDAO().byReferenceInterval(ref, from, to));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }
}
