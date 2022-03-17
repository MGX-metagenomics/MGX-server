package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.commonwl.CommonWL;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.conveyor.XMLValidator;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.dtoadapter.ToolDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.MGXGlobalException;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.UnixHelper;
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
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

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
    TaskHolder taskHolder;
    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    MGXGlobal global;

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
        try {
            Tool tool = mgx.getToolDAO().byJob(job_id);
            return ToolDTOFactory.getInstance().toDTO(tool);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("getDefinition/{id}")
    @Produces("application/x-protobuf")
    public MGXString getDefinition(@PathParam("id") Long tool_id) {
        Tool obj = null;
        try {
            obj = mgx.getToolDAO().getById(tool_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        File f = new File(obj.getFile());
        String fileContents = null;
        try {
            fileContents = UnixHelper.readFile(f);
        } catch (IOException ex) {
            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return MGXString.newBuilder().setValue(fileContents).build();
    }

    @GET
    @Path("listGlobalTools")
    @Produces("application/x-protobuf")
    public ToolDTOList listGlobalTools() {
        try {
            return ToolDTOFactory.getInstance().toDTOList(global.getToolDAO().getAll());
        } catch (MGXGlobalException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ToolDTOList fetchall() {
        try {
            return ToolDTOFactory.getInstance().toDTOList(mgx.getToolDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public ToolDTO fetch(@PathParam("id") Long id) {
        Tool obj = null;
        try {
            obj = mgx.getToolDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ToolDTOFactory.getInstance().toDTO(obj);
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(ToolDTO dto) {
        // not used
        assert false;
        return null;
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId;
        try {
            taskId = taskHolder.addTask(mgx.getToolDAO().delete(id));
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    /*
     * copies global tool into the project database
     */
    @GET
    @Path("installGlobalTool/{global_id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong installGlobalTool(@PathParam("global_id") Long global_id) {
        Tool globalTool = null;
        try {
            globalTool = global.getToolDAO().getById(global_id);
        } catch (MGXGlobalException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        long id;
        try {
            id = mgx.getToolDAO().installGlobalTool(globalTool);
        } catch (MGXException ex) {
            mgx.log(ex);
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXLong.newBuilder().setValue(id).build();
    }

    @GET
    @Path("getAvailableParameters/{id}/{global}")
    @Produces("application/x-protobuf")
    public JobParameterListDTO getAvailableParameters(@PathParam("id") Long id, @PathParam("global") Boolean isGlobalTool) {
        try {
            Tool tool = isGlobalTool ? global.getToolDAO().getById(id) : mgx.getToolDAO().getById(id);
            String toolContent = UnixHelper.readFile(new File(tool.getFile()));
            if (tool.getFile().endsWith("xml")) {
                return getParams(toolContent);
            } else if (tool.getFile().endsWith("cwl")) {
                AutoCloseableIterator<JobParameter> parameters = CommonWL.getParameters(toolContent);
                return JobParameterDTOFactory.getInstance().toDTOList(parameters);
            } else {
                throw new MGXWebException("Unable to determine workflow type.");
            }
        } catch (MGXGlobalException | MGXException | IOException ex) {
            mgx.log(ex);
            throw new MGXWebException(ex.getMessage());
        }
    }

//    @PUT
//    @Path("getAvailableParameters")
//    @Consumes("application/x-protobuf")
//    @Produces("application/x-protobuf")
//    @Deprecated
//    public JobParameterListDTO getAvailableParameters(ToolDTO dto) {
//
//        XMLValidator validator = new XMLValidator();
//        try {
//            if (!validator.isValid(dto.getContent())) {
//                throw new MGXWebException("XML is not Valid");
//            }
//        } catch (SAXException | IOException | ParserConfigurationException ex) {
//            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
//            throw new MGXWebException("XML is not valid: " + ex.getMessage());
//        }
//
//        return getParams(dto.getContent());
//    }
    @PUT
    @Path("getParameters")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public JobParameterListDTO getParameters(MGXString dto) {

        XMLValidator validator = new XMLValidator();
        try {
            if (!validator.isValid(dto.getValue())) {
                throw new MGXWebException("XML is not Valid");
            }
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXWebException("XML is not valid: " + ex.getMessage());
        }

        return getParams(dto.getValue());
    }

    private JobParameterListDTO getParams(String toolContent) {
        AutoCloseableIterator<JobParameter> params;
        try {
            params = JobParameterHelper.getParameters(toolContent, mgxconfig.getPluginDump());
        } catch (MGXException ex) {
            mgx.log(ex);
            throw new MGXWebException(ex.getMessage());
        }
        return JobParameterDTOFactory.getInstance().toDTOList(params);
    }

}
