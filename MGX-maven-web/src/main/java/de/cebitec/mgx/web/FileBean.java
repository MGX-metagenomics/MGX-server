package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dto.dto.FileDTO;
import de.cebitec.mgx.dto.dto.FileDTOList;
import de.cebitec.mgx.dto.dto.FileDTOList.Builder;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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

    @GET
    @Path("fetchall/{baseDir}")
    @Produces("application/x-protobuf")
    public FileDTOList fetchall(@PathParam("baseDir") String baseDir) {
        File currentDirectory = getCurrentDirectory(baseDir);
        return listDir(currentDirectory);
    }

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    public MGXLong create(FileDTO dto) {
        // this method is only used to create directories
        if (!dto.getIsDirectory()) {
            throw new MGXWebException("Invalid input, only directories supported.");
        }

        // security check
        if (dto.getName().contains("..") || !dto.getName().startsWith("." + File.separator)) {
            mgx.log(mgx.getCurrentUser() + " tried to create " + dto.getName());
            throw new MGXWebException("Invalid path.");
        }

        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + dto.getName());
        if (target.exists()) {
            throw new MGXWebException(dto.getName() + " already exists.");
        }
        if (!target.mkdir()) {
            throw new MGXWebException("Could not create " + dto.getName());
        }
        return MGXLong.newBuilder().setValue(1).build();
    }

    @DELETE
    @Path("delete/{path}")
    public Response delete(@PathParam("path") String path) {

        path = path.replace("|", "/");

        // security check
        if (path.contains("..") || !path.startsWith("." + File.separator)) {
            mgx.log(mgx.getCurrentUser() + " tried to delete " + path);
            throw new MGXWebException("Invalid path.");
        }

        File target = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (!target.exists()) {
            throw new MGXWebException("Nonexisting path: " + path);
        }

        deleteHierarchy(target);
        return Response.ok().build();
    }


    private FileDTOList listDir(File base) {
        //
        // This is ugly - application logic shouldn't exist on the
        // presentation (web/REST) layer. On the other hand, the 
        // underlying DAOs/datamodel don't know about the DTOs, i.e.
        // we would need to convert this twice.
        //
        Builder list = FileDTOList.newBuilder();
        for (File f : base.listFiles()) {
            FileDTO.Builder entryBuilder = de.cebitec.mgx.dto.dto.FileDTO.newBuilder();
            entryBuilder.setName(f.getName())
                    .setIsDirectory(f.isDirectory())
                    .setSize(f.isFile() ? f.length() : 0);

            list.addFile(entryBuilder.build());
        }
        return list.build();
    }

    private File getCurrentDirectory(String path) {

        path = path.replace("|", "/");

        if (path.contains("..")) {
            mgx.log(mgx.getCurrentUser() + " tried to access " + path);
            throw new MGXWebException("Invalid path.");
        }

        // validate project directory
        File basedir = new File(mgx.getProjectDirectory() + "files");
        if (!basedir.exists()) {
            basedir.mkdirs();
        }
        if (!basedir.isDirectory()) {
            throw new MGXWebException("File storage subsystem corrupted. Contact server admin.");
        }

        basedir = new File(mgx.getProjectDirectory() + "files" + File.separatorChar + path);
        if (!basedir.isDirectory()) {
            throw new MGXWebException(basedir.getAbsolutePath() + " is not a directory.");
        }
        return basedir;
    }
    
    
    private void deleteHierarchy(File f) {
        if (f.isDirectory()) {
            for (File tmp : f.listFiles()) {
                deleteHierarchy(tmp);
            }
        }
        f.delete();
    }

    private String stripProjectDir(String input) {
        String projectDirectory = mgx.getProjectDirectory();
        if (input.startsWith(projectDirectory)) {
            input = input.substring(projectDirectory.length());
        } else {
            Logger.getLogger(FileBean.class.getName()).log(Level.SEVERE, null, projectDirectory + " not found in " + input);
        }
        return input;
    }
}