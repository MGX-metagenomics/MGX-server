package de.cebitec.mgx.model.dao.workers;

import de.cebitec.mgx.sessions.TaskI;
import java.io.File;
import java.sql.Connection;

/**
 *
 * @author sjaenick
 */
public class DeleteFile extends TaskI {

    private final File file;
    
    public DeleteFile(Connection conn, File f, String projName) {
        super(projName, conn);
        this.file = f;
    }

    @Override
    public void run() {
        setStatus(TaskI.State.PROCESSING, "Deleting..");
        boolean success = deleteHierarchy(file);
        if (success) {
            setStatus(TaskI.State.FINISHED, "File deleted");
        } else {
            setStatus(TaskI.State.FAILED, "Could not delete file.");
        }
    }

    private static boolean deleteHierarchy(File f) {
        boolean ret = true;
        if (f.isDirectory()) {
            for (File tmp : f.listFiles()) {
                ret = ret && deleteHierarchy(tmp);
            }
        }
        return f.delete();
    }
}
