package de.cebitec.mgx.web;

import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.sessions.IteratorHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.util.Iterator;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Iterator")
public class IteratorBean {

    @EJB
    IteratorHolder iterHolder;

    private final static int FETCH_LIMIT = 50_000;

    @GET
    @Path("getLong/{uuid}")
    @Produces("application/x-protobuf")
    public MGXLongList getLong(@PathParam("uuid") String uuid) {
        UUID iterId = UUID.fromString(uuid);
        AutoCloseableIterator<Long> iter = iterHolder.get(iterId);
        if (iter == null) {
            throw new MGXWebException("Invalid iterator ID: " + uuid);
        }

        MGXLongList.Builder b = MGXLongList.newBuilder();
        int cnt = 0;
        while (iter.hasNext() && cnt < FETCH_LIMIT) {
            b.addLong(iter.next());
            cnt++;
        }

        if (iter.hasNext()) {
            b.setComplete(false);
            b.setUuid(uuid);
        } else {
            b.setComplete(true);
            iterHolder.remove(iterId);
            iter.close();
        }
        return b.build();

    }
}
