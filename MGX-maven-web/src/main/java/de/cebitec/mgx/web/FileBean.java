package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto.BytesDTO;
import de.cebitec.mgx.dto.dto.FileDTO;
import de.cebitec.mgx.dto.dto.FileDTOList;
import de.cebitec.mgx.dto.dto.FileDTOList.Builder;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.model.dao.workers.DeleteFile;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.upload.FileUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.io.File;
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
 * @author sj
 */
@Path("File")
@Stateless
public class FileBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-web/UploadSessions")
    UploadSessions sessions;
    @EJB
    TaskHolder taskHolder;

    @GET
    @Path("fetchall/{baseDir}")
    @Produces("application/x-protobuf")
    public FileDTOList fetchall(@PathParam("baseDir") String baseDir) {
        File targetDir = new File(mgx.getProjectDirectory() + "files");
        if (!targetDir.exists()) {
            UnixHelper.createDirectory(targetDir);
        }

        if (!baseDir.startsWith(".")) {
            throw new MGXWebException("Invalid path.");
        }

        File currentDirectory = getCurrentDirectory(baseDir);
        return listDir(currentDirectory);
    }

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXLong create(FileDTO dto) {
        // this method is only used to create directories
        if (!dto.getIsDirectory()) {
            throw new MGXWebException("Invalid input, only directories supported.");
        }

        // security check
        if (dto.getName().contains("..") || !dto.getName().startsWith(".|")) {
            mgx.log(mgx.getCurrentUser() + " tried to create " + dto.getName());
            throw new MGXWebException("Invalid path: " + dto.getName());
        }
        
        String name = dto.getName().substring(2).replace("|", File.separator);
        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + name);
        if (target.exists()) {
            throw new MGXWebException(dto.getName().substring(2) + " already exists.");
        }
        UnixHelper.createDirectory(target);
        if (!target.exists()) {
            throw new MGXWebException("Could not create " + dto.getName());
        }
        return MGXLong.newBuilder().setValue(1).build();
    }

    @DELETE
    @Path("delete/{path}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXString delete(@PathParam("path") String path) {
        // security check
        if (path.contains("..") || !path.startsWith(".")) {
            mgx.log(mgx.getCurrentUser() + " tried to delete " + path);
            throw new MGXWebException("Invalid path: " + path);
        }
        
        if (path.startsWith(".|")) {
            path = path.substring(2);
        }
        
        path = path.replace("|", "/");
        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (!target.exists()) {
            throw new MGXWebException("Nonexisting path: " + path);
        }

        UUID taskId = taskHolder.addTask(new DeleteFile(mgx.getConnection(), target, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("init/{path}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public MGXString init(@PathParam("path") String path) {

        mgx.log("Creating file upload session for " + mgx.getProjectName());

        path = path.replace("|", "/");

        // security check
        if (path.contains("..") || !path.startsWith("." + File.separator)) {
            mgx.log(mgx.getCurrentUser() + " tried to access " + path);
            throw new MGXWebException("Invalid path.");
        }

        File targetDir = new File(mgx.getProjectDirectory() + "files");
        if (!targetDir.exists()) {
            UnixHelper.createDirectory(targetDir);
        }

        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (target.exists()) {
            throw new MGXWebException("File already exists: " + path);
        }

        FileUploadReceiver recv = new FileUploadReceiver(target.getAbsolutePath(), mgx.getProjectName());
        UUID uuid = sessions.registerUploadSession(recv);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("close/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User})
    public Response close(@PathParam("uuid") UUID session_id) {
        try {
            sessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @POST
    @Path("add/{uuid}")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User})
    public Response add(@PathParam("uuid") UUID session_id, BytesDTO data) {
        try {
            sessions.getSession(session_id).add(data.getData().toByteArray());
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("cancel/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User})
    public Response cancel(@PathParam("uuid") UUID session_id) {
        try {
            sessions.cancelSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    private FileDTOList listDir(File baseDir) {
        //
        // This is ugly - application logic shouldn't exist on the
        // presentation (web/REST) layer. On the other hand, the 
        // underlying DAOs/datamodel don't know about the DTOs, i.e.
        // we would need to convert this twice.
        //
        Builder list = FileDTOList.newBuilder();
        for (File f : baseDir.listFiles()) {
            FileDTO.Builder entryBuilder = de.cebitec.mgx.dto.dto.FileDTO.newBuilder();
            String fName = ".|" + stripProjectDir(f.getAbsolutePath());
            entryBuilder.setName(fName.replace("/", "|"))
                    .setIsDirectory(f.isDirectory())
                    .setSize(f.isFile() ? f.length() : 0);

            list.addFile(entryBuilder.build());
        }
        return list.build();
    }

    private File getCurrentDirectory(String path) {

        path = path.substring(1); // remove leading ".|";
        path = path.replace("|", File.separator);

        if (path.contains("..")) {
            mgx.log(mgx.getCurrentUser() + " tried to access " + path);
            throw new MGXWebException("Invalid path.");
        }

        // validate project directory
        File basedir = new File(mgx.getProjectDirectory() + "files");
        if (!basedir.isDirectory()) {
            throw new MGXWebException("File storage subsystem corrupted. Contact server admin.");
        }

        basedir = new File(mgx.getProjectDirectory() + "files" + File.separator + path);
        if (!basedir.isDirectory()) {
            throw new MGXWebException(basedir.getAbsolutePath() + " is not a directory.");
        }
        return basedir;
    }

    private String stripProjectDir(String input) {
        String projectDirectory = mgx.getProjectDirectory() + "files/";
        if (input.startsWith(projectDirectory)) {
            input = input.substring(projectDirectory.length());
        } else {
            mgx.log(projectDirectory + " not found in " + input);
            return "";
        }
        return input;
    }
}
