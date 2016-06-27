package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.BulkObservationDTOList;
import de.cebitec.mgx.dto.dto.ObservationDTO;
import de.cebitec.mgx.dto.dto.ObservationDTOList;
import de.cebitec.mgx.dtoadapter.BulkObservationDTOFactory;
import de.cebitec.mgx.dtoadapter.ObservationDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.misc.SequenceObservation;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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

    @PUT
    @Path("create/{seqId}/{attrId}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response create(@PathParam("seqId") Long seqId, @PathParam("attrId") Long attrId, ObservationDTO dto) {
        try {
            Sequence seq = mgx.getSequenceDAO().getById(seqId);
            Attribute attr = mgx.getAttributeDAO().getById(attrId);
            SequenceObservation obs = ObservationDTOFactory.getInstance().toDB(dto);
            mgx.getObservationDAO().create(obs, seq, attr);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @PUT
    @Path("createBulk")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response createBulk(BulkObservationDTOList dtoList) {
        try {
            mgx.getObservationDAO().createBulk(BulkObservationDTOFactory.getInstance().toDBList(dtoList));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

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

    @DELETE
    @Path("delete/{seqId}/{attrId}/{start}/{stop}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response delete(@PathParam("seqId") Long seqId, @PathParam("attrId") Long attrId, @PathParam("start") Integer start, @PathParam("stop") Integer stop) {
        try {
            mgx.getObservationDAO().delete(seqId, attrId, start, stop);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
}
