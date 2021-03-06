package de.cebitec.mgx.workers;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.TaskI;
import java.io.File;

/**
 *
 * @author sjaenick
 */
public class DeleteFile extends TaskI {

    private final File file;
    
    public DeleteFile(GPMSManagedDataSourceI dataSource, File f, String projName) {
        super(projName, dataSource);
        this.file = f;
    }

    @Override
    public void process() {
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
