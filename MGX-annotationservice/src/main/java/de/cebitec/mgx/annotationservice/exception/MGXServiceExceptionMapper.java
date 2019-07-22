
package de.cebitec.mgx.annotationservice.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author sjaenick
 */
@Provider
public class MGXServiceExceptionMapper implements ExceptionMapper<MGXServiceException> {

    @Override
    public Response toResponse(MGXServiceException ex) {
        return Response.status(ex.status()).
                entity(ex.getMessage()).
                type(MediaType.TEXT_PLAIN).
                build();
    }
}
