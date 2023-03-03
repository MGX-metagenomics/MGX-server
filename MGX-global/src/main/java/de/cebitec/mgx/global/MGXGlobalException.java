package de.cebitec.mgx.global;

import java.io.Serial;

/**
 *
 * @author sjaenick
 */
public class MGXGlobalException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

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
