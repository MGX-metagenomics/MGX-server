package de.cebitec.mgx.web;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.FileDownloadProvider;
import de.cebitec.mgx.dto.dto.BytesDTO;
import de.cebitec.mgx.dto.dto.FileDTO;
import de.cebitec.mgx.dto.dto.FileDTOList;
import de.cebitec.mgx.dto.dto.FileDTOList.Builder;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.upload.FileUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.web.exception.MGXWebException;
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
    @EJB
    UploadSessions sessions;
    @EJB
    DownloadSessions dsessions;
    @EJB
    TaskHolder taskHolder;
    @EJB
    MGXConfigurationI mgxconfig;

    @GET
    @Path("fetchall/{baseDir}")
    @Produces("application/x-protobuf")
    public FileDTOList fetchall(@PathParam("baseDir") String baseDir) {
        if (!baseDir.startsWith(".")) {
            throw new MGXWebException("Invalid path.");
        }

        File currentDirectory;
        try {
            currentDirectory = getCurrentDirectory(baseDir);
            return listDir(currentDirectory);
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
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
        File target;
        try {
            target = new File(mgx.getProjectFileDirectory().getAbsolutePath() + File.separator + name);
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        if (target.exists()) {
            throw new MGXWebException(dto.getName().substring(2) + " already exists.");
        }
        try {
            UnixHelper.createDirectory(target);
        } catch (IOException ex) {
            throw new MGXWebException("Could not create " + dto.getName() + ", " + ex.getMessage());
        }
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
        File target;
        try {
            target = new File(mgx.getProjectFileDirectory().getAbsolutePath() + File.separator + path);
            if (!target.exists()) {
                throw new MGXWebException("Nonexisting path: " + path);
            }
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        
        UUID taskId = taskHolder.addTask(mgx.getFileDAO().delete(target));

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

        File target;
        try {
            target = new File(mgx.getProjectFileDirectory().getAbsolutePath() + File.separator + path);
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
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
    @Produces("application/x-protobuf")
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

        File target;
        try {
            target = new File(mgx.getProjectFileDirectory().getAbsolutePath() + File.separator + path);
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        if (target.exists()) {
            throw new MGXWebException("File already exists: " + path);
        }
        mgx.log(mgx.getCurrentUser() + " initiating file upload to " + target.getAbsolutePath());

        UUID uuid;
        try {
            FileUploadReceiver recv = new FileUploadReceiver(target, mgx.getProjectName());
            uuid = sessions.registerUploadSession(recv);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
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

    private FileDTOList listDir(File baseDir) throws IOException {
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

    private File getCurrentDirectory(String path) throws IOException {

        path = path.substring(1); // remove leading ".|";
        path = path.replace("|", File.separator);

        if (path.contains("..")) {
            mgx.log(mgx.getCurrentUser() + " tried to access " + path);
            throw new MGXWebException("Invalid path.");
        }

        // validate project directory
        File basedir = mgx.getProjectFileDirectory();
        if (!basedir.isDirectory()) {
            throw new MGXWebException("File storage subsystem corrupted. Contact server admin.");
        }

        basedir = new File(mgx.getProjectFileDirectory().getAbsolutePath() + File.separator + path);
        if (!basedir.isDirectory()) {
            // do not expose absolute file system path
            throw new MGXWebException(path + " is not a directory.");
        }
        return basedir;
    }

    private String stripProjectDir(String input) throws IOException {
        String projectDirectory = mgx.getProjectFileDirectory().getAbsolutePath() + File.separator;
        if (input.startsWith(projectDirectory)) {
            input = input.substring(projectDirectory.length());
        } else {
            mgx.log(projectDirectory + " not found in " + input);
            return "";
        }
        return input;
    }
}
