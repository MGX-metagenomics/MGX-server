package de.cebitec.mgx.jobsubmitter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 *
 * @author sjaenick
 */
@Provider
@Consumes(MediaType.TEXT_PLAIN)
public class TextPlainReader implements MessageBodyReader<Boolean> {

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return true;
    }
    private final byte[] buf = new byte[1];

    @Override
    public Boolean readFrom(Class<Boolean> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream in) throws IOException, WebApplicationException {
        in.read(buf);
        
        // check return code of read call
        
        if (buf[0] == '1') {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
