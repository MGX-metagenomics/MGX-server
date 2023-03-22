package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.dto.dto.GeneObservationDTOList;
import de.cebitec.mgx.dtoadapter.GeneObservationDTOFactory;
import de.cebitec.mgx.model.misc.GeneObservation;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("GeneObservation")
public class GeneObservationBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("byGene/{id}")
    @Produces("application/x-protobuf")
    public GeneObservationDTOList byGene(@PathParam("id") Long id) {
        Result<DBIterator<GeneObservation>> iter = mgx.getGeneObservationDAO().byRead(id);
        if (iter.isError()) {
            throw new MGXWebException(iter.getError());
        }
        return GeneObservationDTOFactory.getInstance().toDTOList(iter.getValue());
    }

}
