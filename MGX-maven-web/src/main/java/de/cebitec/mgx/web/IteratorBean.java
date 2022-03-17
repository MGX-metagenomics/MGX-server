package de.cebitec.mgx.web;

import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.sessions.IteratorHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.UUID;

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
