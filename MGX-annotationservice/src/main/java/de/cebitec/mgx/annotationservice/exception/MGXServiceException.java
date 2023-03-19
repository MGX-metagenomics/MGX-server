package de.cebitec.mgx.annotationservice.exception;

import jakarta.ejb.ApplicationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.Serial;

/**
 *
 * @author sjaenick
 */
@ApplicationException
public class MGXServiceException extends WebApplicationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private Status http_status = null;

    public MGXServiceException(String msg, Object... args) {
        this(Status.BAD_REQUEST, String.format(msg, args));
    }

    public MGXServiceException(String message) {
        this(Status.BAD_REQUEST, message);
    }

    public MGXServiceException(Status status, String message) {
        super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build());
        http_status = status;
    }

    public Status status() {
        return http_status != null ? http_status : Status.INTERNAL_SERVER_ERROR;
    }
}
