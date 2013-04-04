
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitter {

    void cancel(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException;

    void delete(MGXController mgx, long jobId) throws MGXDispatcherException, MGXException;
    
    boolean submit(MGXController mgx, long jobId) throws MGXException, MGXDispatcherException;

    boolean validate(MGXController mgx, long jobId) throws MGXInsufficientJobConfigurationException, MGXException;
    
    void shutdown(MGXController mgx) throws MGXDispatcherException;
    
}
