package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SampleDTO;
import de.cebitec.mgx.dto.dto.SampleDTOList;
import de.cebitec.mgx.dtoadapter.SampleDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

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
    @EJB
    TaskHolder taskHolder;
    @EJB
    MappingSessions mappingSessions;

    @PUT
    @Path("create")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(SampleDTO dto) {
//        Habitat h = null;
//        try {
//            h = mgx.getHabitatDAO().getById(dto.getHabitatId());
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
        Sample s = SampleDTOFactory.getInstance().toDB(dto);
        //h.addSample(s);
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(SampleDTO dto) {
        Sample sample = SampleDTOFactory.getInstance().toDB(dto);
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
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SampleDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public SampleDTOList fetchall() {
        try {
            return SampleDTOFactory.getInstance().toDTOList(mgx.getSampleDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("byHabitat/{id}")
    @Produces("application/x-protobuf")
    public SampleDTOList byHabitat(@PathParam("id") Long hab_id) {
        AutoCloseableIterator<Sample> samples;
        try {
            // make sure the habitat exists
            Habitat obj = mgx.getHabitatDAO().getById(hab_id);
            samples = mgx.getSampleDAO().byHabitat(hab_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SampleDTOFactory.getInstance().toDTOList(samples);
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {

        UUID taskId;
        try {
            Sample obj = mgx.getSampleDAO().getById(id);
            taskId = taskHolder.addTask(mgx.getSampleDAO().delete(id));
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }
}
