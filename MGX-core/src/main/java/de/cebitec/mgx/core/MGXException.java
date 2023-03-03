package de.cebitec.mgx.core;

import java.io.Serial;

/**
 *
 * @author sjaenick
 */
public class MGXException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

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
