package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dto.dto.FileDTO;
import de.cebitec.mgx.dto.dto.FileDTOList;
import de.cebitec.mgx.dto.dto.FileDTOList.Builder;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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