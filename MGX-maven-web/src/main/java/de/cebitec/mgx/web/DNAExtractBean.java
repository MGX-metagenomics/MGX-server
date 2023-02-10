package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.dto.DNAExtractDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.DNAExtractDTOFactory;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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
@Path("DNAExtract")
public class DNAExtractBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(DNAExtractDTO dto) {
        long DNAExtract_id = -1;
        try {
            //Sample s = mgx.getSampleDAO().getById(dto.getSampleId());
            DNAExtract d = DNAExtractDTOFactory.getInstance().toDB(dto);
            //s.addDNAExtract(d);
            DNAExtract_id = mgx.getDNAExtractDAO().create(d);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(DNAExtract_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(DNAExtractDTO dto) {
        DNAExtract extract = DNAExtractDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getDNAExtractDAO().update(extract);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTO fetch(@PathParam("id") Long id) {
        DNAExtract obj = null;
        try {
            obj = mgx.getDNAExtractDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return DNAExtractDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList fetchall() {
        try {
            return DNAExtractDTOFactory.getInstance().toDTOList(mgx.getDNAExtractDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("bySample/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList bySample(@PathParam("id") Long sample_id) {
        try {
            return DNAExtractDTOFactory.getInstance().toDTOList(mgx.getDNAExtractDAO().bySample(sample_id));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {

        UUID taskId;
        try {
            DNAExtract obj = mgx.getDNAExtractDAO().getById(id);
            taskId = taskHolder.addTask(mgx.getDNAExtractDAO().delete(id));
        } catch (MGXException | IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }
}
