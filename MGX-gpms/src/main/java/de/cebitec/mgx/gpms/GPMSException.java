package de.cebitec.mgx.gpms;

/**
 *
 * @author sjaenick
 */
public class GPMSException extends Exception {

    public GPMSException(Throwable cause) {
        super(cause);
    }

    public GPMSException(String msg) {
        super(msg);
    }

    public GPMSException(String msg, Object... args) {
        super(String.format(msg, args));
    }
}
