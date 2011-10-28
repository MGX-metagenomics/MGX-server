
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;

/**
 *
 * @author sjaenick
 */
public interface JobSubmitter {

    boolean cancel(MGXController mgx, Long jobId) throws MGXDispatcherException, MGXException;

    void delete(MGXController mgx, Long jobId) throws MGXDispatcherException, MGXException;

    boolean submit(MGXController mgx, Long jobId) throws MGXException, MGXDispatcherException;

    boolean verify(MGXController mgx, Long jobId) throws MGXInsufficientJobConfigurationException, MGXException;
    
}
