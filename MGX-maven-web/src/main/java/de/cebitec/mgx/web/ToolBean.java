package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.dto.dto.ToolDTOList.Builder;
import de.cebitec.mgx.dtoadapter.ToolDTOFactory;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Tool")
public class ToolBean {

    @Inject
    @MGX
    MGXController mgx;

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    public MGXLong create(ToolDTO dto) {
        Tool h = ToolDTOFactory.getInstance().toDB(dto, false);
        Long id;
        try {
            id = mgx.getToolDAO().create(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(id).build();
    }

    @GET
    @Path("listGlobalTools")
    @Produces("application/x-protobuf")
    public ToolDTOList listGlobalTools() {
        Builder b = ToolDTOList.newBuilder();
        for (Object o : mgx.getGlobal().getToolDAO().getAll()) {
            b.addTool(ToolDTOFactory.getInstance().toDTO((Tool) o));
        }
        return b.build();
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ToolDTOList fetchall() {
        Builder b = ToolDTOList.newBuilder();
        for (Object o : mgx.getToolDAO().getAll()) {
            b.addTool(ToolDTOFactory.getInstance().toDTO((Tool) o));
        }
        return b.build();
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getToolDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    /*
     * copies global tool into the project database
     */
    @PUT
    @Path("installTool")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong installTool(MGXLong global_id) {
        Tool globalTool = null;
        try {
            globalTool = (Tool) mgx.getGlobal().getToolDAO().getById(global_id.getValue());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        long id;
        try {
            id = mgx.getToolDAO().installGlobalTool(globalTool, globalTool.getClass(), mgx.getProjectDirectory());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXLong.newBuilder().setValue(id).build();
    }
}
