package de.cebitec.mgx.web;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.FileDownloadProvider;
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
    DownloadSessions dsessions;
    @EJB
    TaskHolder taskHolder;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;

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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(FileDTO dto) {
        // this method is only used to create directories
        if (!dto.getIsDirectory()) {
            throw new MGXWebException("Invalid input, only directories supported.");
        }

        // security check
        if (dto.getName().contains("..") || !dto.getName().startsWith(".|")) {
            mgx.log("tried to create invalid directory " + dto.getName());
            throw new MGXWebException("Invalid path: " + dto.getName());
        }

        String name = dto.getName().substring(2).replace("|", File.separator);
        File target = new File(mgx.getProjectDirectory() + "files" + File.separator + name);
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("path") String path) {
        // security check
        if (path.contains("..") || !path.startsWith(".")) {
            mgx.log("tried to delete invalid " + path);
            throw new MGXWebException("Invalid path: " + path);
        }

        if (path.startsWith(".|")) {
            path = path.substring(2);
        }

        path = path.replace("|", File.separator);
        File target = new File(mgx.getProjectDirectory() + "files" + File.separator + path);
        if (!target.exists()) {
            throw new MGXWebException("Nonexisting path: " + path);
        }

        UUID taskId = taskHolder.addTask(new DeleteFile(mgx.getConnection(), target, mgx.getProjectName()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    /*
     * file download interface
     */
    @GET
    @Path("initDownload/{path}")
    @Produces("application/x-protobuf")
    public MGXString initDownload(@PathParam("path") String path) {
        path = path.replace("|", File.separator);

        // security check
        if (path.contains("..") || !path.startsWith("." + File.separator)) {
            mgx.log("tried to access invalid path " + path);
            throw new MGXWebException("Invalid path.");
        }

        if (path.startsWith("." + File.separator)) {
            path = path.substring(2);
        }

        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (!target.exists()) {
            mgx.log("tried to download non-existing path " + path);
            throw new MGXWebException("File does not exist: " + path);
        }
        mgx.log("initiating file download for " + target.getAbsolutePath());
        FileDownloadProvider fdp = new FileDownloadProvider(mgx.getProjectName(), target);

        UUID uuid = dsessions.registerDownloadSession(fdp);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("get/{uuid}")
    @Consumes("application/x-protobuf")
    public BytesDTO get(@PathParam("uuid") UUID session_id) {
        try {
            DownloadProviderI<byte[]> dp = dsessions.<byte[]>getSession(session_id);
            return BytesDTO.newBuilder().setData(ByteString.copyFrom(dp.fetch())).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
            dsessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("initPluginDownload")
    @Produces("application/x-protobuf")
    public MGXString initPluginDownload() {
        File target = mgxconfig.getPluginDump();
        mgx.log("initiating file download for " + target.getAbsolutePath());
        FileDownloadProvider fdp = new FileDownloadProvider(mgx.getProjectName(), target);

        UUID uuid = dsessions.registerDownloadSession(fdp);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    /*
     * file upload interface
     */
    @GET
    @Path("initUpload/{path}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString initUpload(@PathParam("path") String path) {
        path = path.replace("|", File.separator);

        // security check
        if (path.contains("..") || !path.startsWith("." + File.separator)) {
            mgx.log(mgx.getCurrentUser() + " tried to access " + path);
            throw new MGXWebException("Invalid path.");
        }

        if (path.startsWith("." + File.separator)) {
            path = path.substring(2);
        }

        File targetDir = new File(mgx.getProjectDirectory() + "files");
        if (!targetDir.exists()) {
            UnixHelper.createDirectory(targetDir);
        }

        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (target.exists()) {
            throw new MGXWebException("File already exists: " + path);
        }
        mgx.log(mgx.getCurrentUser() + " initiating file upload to " + target.getAbsolutePath());

        FileUploadReceiver recv = new FileUploadReceiver(target.getAbsolutePath(), mgx.getProjectName());
        UUID uuid = sessions.registerUploadSession(recv);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeUpload/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response closeUpload(@PathParam("uuid") UUID session_id) {
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
