package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.Directory;
import de.cebitec.mgx.dto.dto.FileOrDirectory;
import de.cebitec.mgx.dto.dto.FoDList;
import de.cebitec.mgx.dto.dto.FoDList.Builder;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public FoDList fetchall() {
        File basedir = new File(mgx.getProjectDirectory());
        if (!basedir.exists()) {
            basedir.mkdirs();
        }
        if (!basedir.isDirectory()) {
            throw new MGXWebException("File storage subsystem corrupted. Contact server admin.");
        }
        return listDir(basedir);
    }

    private FoDList listDir(File base) {
        //
        // This is ugly - application logic shouldn't exist on the
        // presentation (web/REST) layer. On the other hand, the 
        // underlying DAOs/datamodel don't know about the DTOs, i.e.
        // we would need to convert this twice.
        //
        // FIXME: Move this to the DTOAdapter package?
        //
        Builder list = FoDList.newBuilder();
        for (File f : base.listFiles()) {
            FileOrDirectory.Builder entryBuilder = de.cebitec.mgx.dto.dto.FileOrDirectory.newBuilder();
            if (f.isFile()) {
                dto.File.Builder fileBuilder = de.cebitec.mgx.dto.dto.File.newBuilder();
                try {
                    fileBuilder.setName(stripProjectDir(f.getCanonicalPath()));
                } catch (IOException ex) {
                    Logger.getLogger(FileBean.class.getName()).log(Level.SEVERE, null, ex);
                }
                fileBuilder.setSize(f.length());
                entryBuilder.setFile(fileBuilder.build());
            } else if (f.isDirectory()) {
                Directory.Builder dirBuilder = de.cebitec.mgx.dto.dto.Directory.newBuilder();
                try {
                    dirBuilder.setName(stripProjectDir(f.getCanonicalPath()));
                } catch (IOException ex) {
                    Logger.getLogger(FileBean.class.getName()).log(Level.SEVERE, null, ex);
                }
                // recurse into subdirectory
                List<FileOrDirectory> entryList = listDir(f.getAbsoluteFile()).getEntryList();
                dirBuilder.addAllFile(entryList);
                entryBuilder.setDirectory(dirBuilder.build());
            }
            list.addEntry(entryBuilder.build());
        }
        return list.build();
    }
    
    private String stripProjectDir(String input) {
        String projectDirectory = mgx.getProjectDirectory();
        if (input.startsWith(projectDirectory)) {
            input = input.substring(projectDirectory.length());
        } else {
            System.err.println(projectDirectory +" not found in "+ input);
        }
        return input;
    }
}