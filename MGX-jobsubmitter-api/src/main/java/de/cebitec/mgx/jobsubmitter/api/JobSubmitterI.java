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
public interface JobSubmitterI {

    void cancel(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException;

    void delete(Host dispatcherHost,String projectName, long jobId) throws MGXDispatcherException;

    boolean submit(Host dispatcherHost, String projectName, DataSource dataSource, Job job) throws MGXDispatcherException;

    boolean validate(Host dispatcherHost, String projectName, DataSource dataSource, final Job job, String dbHost, String dbName, String dbUser, String dbPass, File projDir) throws MGXInsufficientJobConfigurationException, MGXDispatcherException;

    void shutdown(Host dispatcherHost, String token) throws MGXDispatcherException;

}
