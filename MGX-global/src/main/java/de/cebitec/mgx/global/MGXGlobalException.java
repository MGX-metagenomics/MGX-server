package de.cebitec.mgx.global;

/**
 *
 * @author sjaenick
 */
public class MGXGlobalException extends Exception {

    public MGXGlobalException(Throwable cause) {
        super(cause);
    }

    public MGXGlobalException(String msg) {
        super(msg);
    }

    public MGXGlobalException(String msg, Object... args) {
        super(String.format(msg, args));
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
