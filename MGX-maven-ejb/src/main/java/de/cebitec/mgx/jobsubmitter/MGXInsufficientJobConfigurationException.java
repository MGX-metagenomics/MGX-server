package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.controller.MGXException;

/**
 *
 * @author sjaenick
 */
public class MGXInsufficientJobConfigurationException extends MGXException {

    public MGXInsufficientJobConfigurationException(Throwable cause) {
        super(cause);
    }

    public MGXInsufficientJobConfigurationException(String msg) {
        super(msg);
    }
}
