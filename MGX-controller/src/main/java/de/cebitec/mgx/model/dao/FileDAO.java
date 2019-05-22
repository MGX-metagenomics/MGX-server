package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.workers.DeleteFile;
import java.io.File;

/**
 *
 * @author sjaenick
 */
public class FileDAO  {
    
    private final MGXController ctx;

    public FileDAO(MGXController ctx) {
        this.ctx = ctx;
    }

    public TaskI delete(File f) {
        return new DeleteFile(ctx.getDataSource(),f, ctx.getProjectName());
    }
}
