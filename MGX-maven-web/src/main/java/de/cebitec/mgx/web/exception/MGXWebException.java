package de.cebitec.mgx.web.exception;

import jakarta.ejb.ApplicationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


/**
 *
 * @author sjaenick
 */
@ApplicationException
public class MGXWebException extends WebApplicationException {

    private Status http_status = null;

    public MGXWebException(String msg, Object... args) {
        this(Status.BAD_REQUEST, String.format(msg, args));
    }

    public MGXWebException(String message) {
        this(Status.BAD_REQUEST, message);
    }

    public MGXWebException(Status status, String message) {
        super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build());
        assert message != null;
        assert !"".equals(message);
        http_status = status;
    }

    public Status status() {
        return http_status != null ? http_status : Status.INTERNAL_SERVER_ERROR;
    }
}
