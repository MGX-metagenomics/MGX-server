package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.PointDTOList;
import de.cebitec.mgx.dtoadapter.PointDTOFactory;
import de.cebitec.mgx.statistics.Rarefaction;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.Point;
import de.cebitec.mgx.web.exception.MGXWebException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author sj
 */
@Path("Statistics")
@Stateless
public class StatisticsBean {

    @Inject
    @MGX
    MGXController mgx;

    @EJB
    Rarefaction rarefaction;

    @PUT
    @Path("Rarefaction")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PointDTOList create(MGXLongList dto) {
        long[] data = new long[dto.getLongCount()];
        for (int i = 0; i < dto.getLongCount(); i++) {
            data[i] = dto.getLong(i).getValue();
        }
        AutoCloseableIterator<Point> ret;
        try {
            ret = rarefaction.rarefy(data);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PointDTOFactory.getInstance().toDTOList(ret);
    }
}
