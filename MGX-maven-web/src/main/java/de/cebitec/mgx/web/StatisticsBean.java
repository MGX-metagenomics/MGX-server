package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.MGXMatrixDTO;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.PointDTOList;
import de.cebitec.mgx.dto.dto.ProfileDTO;
import de.cebitec.mgx.dtoadapter.PointDTOFactory;
import de.cebitec.mgx.model.misc.NamedVector;
import de.cebitec.mgx.statistics.Clustering;
import de.cebitec.mgx.statistics.Rarefaction;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.Point;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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

    @EJB
    Rarefaction rarefaction;
    @EJB
    Clustering clust;

    @PUT
    @Path("Rarefaction")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PointDTOList rarefy(MGXLongList dto) {
        double[] data = new double[dto.getLongCount()];
        for (int i = 0; i < dto.getLongCount(); i++) {
            data[i] = (double) dto.getLong(i);
        }
        AutoCloseableIterator<Point> ret;
        try {
            ret = rarefaction.rarefy(data);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PointDTOFactory.getInstance().toDTOList(ret);
    }

    @PUT
    @Path("Clustering")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXString cluster(MGXMatrixDTO dto) {
        Set<NamedVector> data = new HashSet<>();
        for (ProfileDTO pdto : dto.getRowList()) {
            List<Long> ll = pdto.getValues().getLongList();
            long[] tmp = new long[ll.size()];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = ll.get(i);
            }
            data.add(new NamedVector(pdto.getName(), tmp));
        }
        String ret;
        try {
            ret = clust.cluster(data);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXString.newBuilder().setValue(ret).build();
    }
}
