package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.dtoadapter.ToolDTOFactory;
import de.cebitec.mgx.jobsubmitter.JobParameterHelper;
import de.cebitec.mgx.model.dao.deleteworkers.DeleteTool;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import de.cebitec.mgx.web.helper.XMLValidator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
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
    JobParameterHelper paramHelper;
    @EJB
    TaskHolder taskHolder;

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
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
    @Secure(rightsNeeded = {MGXRoles.User})
    public Response update(ToolDTO dto) {
        // not used
        assert false;
        return null;
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId = taskHolder.addTask(new DeleteTool(mgx.getConnection(), id, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    /*
     * copies global tool into the project database
     */
    @GET
    @Path("installGlobalTool/{global_id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXLong installGlobalTool(@PathParam("global_id") Long global_id) {
        Tool globalTool = null;
        try {
            globalTool = mgx.getGlobal().getToolDAO().getById(global_id);
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
    public JobParameterListDTO getAvailableParameters(@PathParam("id") Long id, @PathParam("global") Boolean global) {
        try {
            Tool tool = global ? mgx.getGlobal().getToolDAO().getById(id) : mgx.getToolDAO().getById(id);
            String toolXMLData = readFile(new File(tool.getXMLFile()));
            return getParams(toolXMLData);
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
    }

    @PUT
    @Path("getAvailableParameters")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public JobParameterListDTO getAvailableParameters(ToolDTO dto) {

        XMLValidator validator = new XMLValidator();
        try {
            if (!validator.isValid(dto.getXml())) {
                throw new MGXWebException("XML is not Valid");
            }
        } catch (SAXException ex) {
            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXWebException("XML is not valid: " + ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXWebException("XML is not valid: " + ex.getMessage());
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ToolBean.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXWebException("XML is not valid: " + ex.getMessage());
        }


        return getParams(dto.getXml());
    }

    private JobParameterListDTO getParams(String XMLData) {
        File plugins = mgx.getConfiguration().getPluginDump();
        AutoCloseableIterator<JobParameter> params = paramHelper.getParameters(XMLData, plugins);
        return JobParameterDTOFactory.getInstance().toDTOList(params);
    }

    private static String readFile(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
