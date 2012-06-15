package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.dtoadapter.ToolDTOFactory;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
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
        Tool t = ToolDTOFactory.getInstance().toDB(dto, false);
        Long id = null;

        try {
            id = mgx.getToolDAO().create(t);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXLong.newBuilder().setValue(id).build();
    }

    @GET
    @Path("byJob/{id}")
    @Produces("application/x-protobuf")
    public ToolDTO byJob(@PathParam("id") Long job_id) {
        Job job;
        try {
            job = mgx.getJobDAO().getById(job_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ToolDTOFactory.getInstance().toDTO(job.getTool());
    }

    @GET
    @Path("listGlobalTools")
    @Produces("application/x-protobuf")
    public ToolDTOList listGlobalTools() {
        return ToolDTOFactory.getInstance().toDTOList(mgx.getGlobal().getToolDAO().getAll());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ToolDTOList fetchall() {
        return ToolDTOFactory.getInstance().toDTOList(mgx.getToolDAO().getAll());
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

        System.err.println("installed global tool to project with id " + id);

        return MGXLong.newBuilder().setValue(id).build();
    }
}
