package de.cebitec.mgx.jobsubmitter.api;

import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.dispatcher.common.MGXInsufficientJobConfigurationException;
import de.cebitec.mgx.model.db.Job;
import java.io.File;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitter {

    void cancel(String projectName, long jobId) throws MGXDispatcherException;

    void delete(String projectName, long jobId) throws MGXDispatcherException;

    boolean submit(String dispatcherHost, DataSource dataSource, String projName, Job job) throws MGXDispatcherException;

    boolean validate(String projName, DataSource dataSource, final Job job, String dispatcherHost, String dbHost, String dbName, String dbUser, String dbPass, File projDir) throws MGXInsufficientJobConfigurationException, MGXDispatcherException;

    void shutdown(String dispatcherHost, String token) throws MGXDispatcherException;

}
