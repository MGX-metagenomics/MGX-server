package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.SampleDTO;
import de.cebitec.mgx.dto.dto.SampleDTOList;
import de.cebitec.mgx.dtoadapter.SampleDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
@Stateless
@Path("Sample")
public class SampleBean {

    @Inject
    @MGX
    MGXController mgx;

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    public MGXLong create(SampleDTO dto) {
        Habitat h = null;
        try {
            h = mgx.getHabitatDAO().getById(dto.getHabitatId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        Sample s = SampleDTOFactory.getInstance().toDB(dto);
        h.addSample(s);
        long sample_id;
        try {
            sample_id = mgx.getSampleDAO().create(s);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(sample_id).build();
    }

    @POST
    @Path("update")
    public Response update(SampleDTO dto) {
        Habitat h = null;
        try {
            h = mgx.getHabitatDAO().getById(dto.getHabitatId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        Sample sample = SampleDTOFactory.getInstance().toDB(dto);
        sample.setHabitat(h);
        try {
            mgx.getSampleDAO().update(sample);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public SampleDTO fetch(@PathParam("id") Long id) {
        Sample obj;
        try {
            obj = mgx.getSampleDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException("No such Sample");
        }
        return SampleDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public SampleDTOList fetchall() {
        return SampleDTOFactory.getInstance().toDTOList(mgx.getSampleDAO().getAll());
    }

    @GET
    @Path("byHabitat/{id}")
    @Produces("application/x-protobuf")
    public SampleDTOList byHabitat(@PathParam("id") Long hab_id) {
        Habitat habitat;
        try {
            habitat = mgx.getHabitatDAO().getById(hab_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        AutoCloseableIterator<Sample> samples = mgx.getSampleDAO().byHabitat(habitat);
        return SampleDTOFactory.getInstance().toDTOList(samples);
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getSampleDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
}
