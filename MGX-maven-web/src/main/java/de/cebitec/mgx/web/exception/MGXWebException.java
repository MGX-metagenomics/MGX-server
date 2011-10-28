package de.cebitec.mgx.web.exception;

import javax.ejb.ApplicationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author sjaenick
 */
@ApplicationException
public class MGXWebException extends WebApplicationException {

    private Status http_status = null;

//    public MGXWebException(Throwable cause) {
//        super(cause);
//    }
    public MGXWebException(String message) {
        super(Response.status(Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN).build());
    }

    public MGXWebException(Status status, String message) {
        super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build());
        http_status = status;
    }

    public Status status() {
        if (http_status != null) {
            return http_status;
        } else {
            return Status.INTERNAL_SERVER_ERROR;
        }
    }
}
