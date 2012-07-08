package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.dtoadapter.ToolDTOFactory;
import de.cebitec.mgx.jobsubmitter.JobParameterHelper;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.List;
import javax.ejb.EJB;
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
    @EJB
    JobParameterHelper paramHelper;

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
            globalTool = mgx.getGlobal().getToolDAO().getById(global_id.getValue());
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

    @GET
    @Path("getAvailableParameters/{id}/{global}")
    @Produces("application/x-protobuf")
    public dto.JobParameterListDTO getAvailableParameters(@PathParam("id") Long id, @PathParam("global") Boolean global) {

        try {
            Tool tool = global ? mgx.getGlobal().getToolDAO().getById(id) : mgx.getToolDAO().getById(id);
            String toolFile = tool.getXMLFile();
            String plugins = mgx.getConfiguration().getPluginDump();
            List<JobParameter> params = paramHelper.getParameters(toolFile, plugins);

            return JobParameterDTOFactory.getInstance().toDTOList(params);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }
}
