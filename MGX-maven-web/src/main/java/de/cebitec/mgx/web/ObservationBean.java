package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.ObservationDTOList;
import de.cebitec.mgx.dtoadapter.ObservationDTOFactory;
import de.cebitec.mgx.model.misc.SequenceObservation;
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
@Path("Observation")
public class ObservationBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("byRead/{id}")
    @Produces("application/x-protobuf")
    public ObservationDTOList byRead(@PathParam("id") Long id) {
        try {
            DBIterator<SequenceObservation> iter = mgx.getObservationDAO().byRead(id);
            return ObservationDTOFactory.getInstance().toDTOList(iter);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }
}
