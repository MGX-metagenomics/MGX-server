package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.GeneObservationDTOList;
import de.cebitec.mgx.dtoadapter.GeneObservationDTOFactory;
import de.cebitec.mgx.model.misc.GeneObservation;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
        try {
            DBIterator<GeneObservation> iter = mgx.getGeneObservationDAO().byRead(id);
            return GeneObservationDTOFactory.getInstance().toDTOList(iter);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }
   
}
