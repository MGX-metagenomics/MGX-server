package de.cebitec.mgx.jobsubmitter.api;

import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitterI {

    void cancel(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException;

    void delete(Host dispatcherHost,String projectName, long jobId) throws MGXDispatcherException;

    boolean submit(Host dispatcherHost, String projectName, long jobId) throws MGXDispatcherException;

    boolean validate(Host dispatcherHost, String projName, long jobId) throws MGXDispatcherException;

    void shutdown(Host dispatcherHost, String token) throws MGXDispatcherException;

}
