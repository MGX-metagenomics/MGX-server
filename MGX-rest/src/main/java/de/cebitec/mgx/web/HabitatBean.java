package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.HabitatDTO;
import de.cebitec.mgx.dto.dto.HabitatDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.HabitatDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.sessions.TaskHolder;
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
import java.io.IOException;
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
@Path("Habitat")
@Stateless
public class HabitatBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getHabitatDAO().create(h);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getHabitatDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public HabitatDTO fetch(@PathParam("id") Long id) {
        Result<Habitat> obj = mgx.getHabitatDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return HabitatDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public HabitatDTOList fetchall() {
        Result<AutoCloseableIterator<Habitat>> obj = mgx.getHabitatDAO().getAll();
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return HabitatDTOFactory.getInstance().toDTOList(obj.getValue());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {

        // verify the habitat exists
        Result<Habitat> obj = mgx.getHabitatDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }

        UUID taskId;
        try {
            Result<TaskI> delete = mgx.getHabitatDAO().delete(id);
            if (delete.isError()) {
                throw new MGXWebException(delete.getError());
            }
            taskId = taskHolder.addTask(delete.getValue());
        } catch (IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();

    }
}
