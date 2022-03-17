
package de.cebitec.mgx.annotationservice.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


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
