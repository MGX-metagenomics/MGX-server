package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.BinDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.BinDTOFactory;
import de.cebitec.mgx.model.db.Bin;
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
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
@Path("Bin")
@Stateless
public class BinBean {

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
    public MGXLong create(BinDTO dto) {
        Bin x = BinDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getBinDAO().create(x);
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
    public Response update(BinDTO dto) {
        Bin h = BinDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getBinDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public BinDTO fetch(@PathParam("id") Long id) {
        Result<Bin> obj = mgx.getBinDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return BinDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public BinDTOList fetchall() {
        Result<AutoCloseableIterator<Bin>> bins = mgx.getBinDAO().getAll();
        if (bins.isError()) {
            throw new MGXWebException(bins.getError());
        }
        return BinDTOFactory.getInstance().toDTOList(bins.getValue());
    }

    @GET
    @Path("byAssembly/{id}")
    @Produces("application/x-protobuf")
    public BinDTOList byAssembly(@PathParam("id") Long asm_id) {
        Result<AutoCloseableIterator<Bin>> bins = mgx.getBinDAO().byAssembly(asm_id);
        if (bins.isError()) {
            throw new MGXWebException(bins.getError());
        }
        return BinDTOFactory.getInstance().toDTOList(bins.getValue());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        Result<TaskI> delete = mgx.getBinDAO().delete(id);
        if (delete.isError()) {
            throw new MGXWebException(delete.getError());
        }
        UUID taskId = taskHolder.addTask(delete.getValue());
        return MGXString.newBuilder().setValue(taskId.toString()).build();

    }
}
