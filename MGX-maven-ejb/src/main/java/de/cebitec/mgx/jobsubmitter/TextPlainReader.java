package de.cebitec.mgx.jobsubmitter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

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
    private byte[] buf = new byte[1];

    @Override
    public Boolean readFrom(Class<Boolean> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, String> mm, InputStream in) throws IOException, WebApplicationException {
        in.read(buf);
        
        if (buf[0] == '1') {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
