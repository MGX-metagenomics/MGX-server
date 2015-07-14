package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.model.db.Job;
import java.io.File;
import java.sql.Connection;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitter {

    void cancel(String projectName, long jobId) throws MGXDispatcherException;

    void delete(String projectName, long jobId) throws MGXDispatcherException;

    //boolean submit(MGXController mgx, long jobId) throws MGXException, MGXDispatcherException;
    boolean submit(String dispatcherHost, Connection conn, String projName, Job job) throws MGXDispatcherException;

    boolean validate(MGXController mgx, Connection conn, String projName, long jobId) throws MGXInsufficientJobConfigurationException, MGXDispatcherException;

    boolean validate(String projName, Connection conn, final Job job, MGXConfiguration config, String dbHost, String dbName, File projDir) throws MGXInsufficientJobConfigurationException, MGXDispatcherException;

    void shutdown(String dispatcherHost, String token) throws MGXDispatcherException;

}
