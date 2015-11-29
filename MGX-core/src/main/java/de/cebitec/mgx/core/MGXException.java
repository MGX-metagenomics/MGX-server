package de.cebitec.mgx.core;

/**
 *
 * @author sjaenick
 */
public class MGXException extends Exception {

    public MGXException(Throwable cause) {
        super(cause);
    }

    public MGXException(String msg) {
        super(msg);
    }

    public MGXException(String msg, Object... args) {
        super(String.format(msg, args));
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
