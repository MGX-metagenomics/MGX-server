package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import java.sql.Connection;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitter {

    void cancel(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException;

    void delete(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException;

    //boolean submit(MGXController mgx, long jobId) throws MGXException, MGXDispatcherException;
    boolean submit(String dispatcherHost, Connection conn, String projName, Job job) throws MGXException, MGXDispatcherException;

    boolean validate(MGXController mgx, long jobId) throws MGXInsufficientJobConfigurationException, MGXException;

    boolean validate(String projName, Connection conn, final Job job, MGXConfiguration config, String dbHost, String dbName, String projDir) throws MGXInsufficientJobConfigurationException, MGXException;

    void shutdown(MGXController mgx) throws MGXDispatcherException;

}
