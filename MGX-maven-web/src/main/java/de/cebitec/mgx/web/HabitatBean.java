package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.HabitatDTO;
import de.cebitec.mgx.dto.dto.HabitatDTOList;
import de.cebitec.mgx.dto.dto.HabitatDTOList.Builder;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dtoadapter.HabitatDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Path("Habitat")
@Stateless
public class HabitatBean {

    @Inject
    @MGX
    MGXController mgx;

//    @GET
//    @Path("PING/{id}")
//    @Consumes("application/x-protobuf")
//    @Produces("application/x-protobuf")
//    public MGXLong ping(@PathParam("id") Long in) {
//        return MGXLong.newBuilder().setValue(in).build();
//    }

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong create(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getHabitatDAO().create(h);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public Response update(HabitatDTO dto) {
        Habitat h = HabitatDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getHabitatDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public HabitatDTO fetch(@PathParam("id") Long id) {
        Habitat obj = null;
        try {
            obj = mgx.getHabitatDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return HabitatDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public HabitatDTOList fetchall() {
        Builder b = HabitatDTOList.newBuilder();
        for (Habitat o : mgx.getHabitatDAO().getAll()) {
            b.addHabitat(HabitatDTOFactory.getInstance().toDTO(o));
        }
        return b.build();
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getHabitatDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
}
