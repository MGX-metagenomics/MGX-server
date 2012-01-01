package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Job;
import java.io.File;

/**
 *
 * @author sjaenick
 */
public class JobDAO<T extends Job> extends DAO<T> {

    private static String[] suffices = {"", ".xml", ".stdout", ".stderr"};

    @Override
    Class getType() {
        return Job.class;
    }

    @Override
    public void delete(Long id) {
        
        /*
         * here, we only delete the persistent files that might have
         * been created by executing a job. The dispatcher will handle
         * deletion of generated observations and orphan attributes
         */
        
        StringBuilder sb = new StringBuilder(getController().getProjectDirectory())
                .append(File.separator)
                .append(id);

        boolean all_deleted = true;
        for (String suffix : suffices) {
            File f = new File(sb.toString() + suffix);
            if (f.exists())  {
                boolean deleted = f.delete();
                all_deleted = all_deleted && deleted;
            }
        }
    }
}
