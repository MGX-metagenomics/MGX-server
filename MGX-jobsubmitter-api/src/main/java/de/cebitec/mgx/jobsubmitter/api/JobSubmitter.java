package de.cebitec.mgx.jobsubmitter.api;

import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.dispatcher.common.MGXInsufficientJobConfigurationException;
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

    boolean submit(String dispatcherHost, Connection conn, String projName, Job job) throws MGXDispatcherException;

    boolean validate(String projName, Connection conn, final Job job, String dispatcherHost, String dbHost, String dbName, String dbUser, String dbPass, File projDir) throws MGXInsufficientJobConfigurationException, MGXDispatcherException;

    void shutdown(String dispatcherHost, String token) throws MGXDispatcherException;

}
