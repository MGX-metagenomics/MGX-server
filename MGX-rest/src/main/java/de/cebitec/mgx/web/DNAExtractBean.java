package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.dto.DNAExtractDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.DNAExtractDTOFactory;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
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
        Result<DNAExtract> obj = mgx.getDNAExtractDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return DNAExtractDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList fetchall() {
        Result<AutoCloseableIterator<DNAExtract>> all = mgx.getDNAExtractDAO().getAll();
        if (all.isError()) {
            throw new MGXWebException(all.getError());
        }
        return DNAExtractDTOFactory.getInstance().toDTOList(all.getValue());
    }

    @GET
    @Path("bySample/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList bySample(@PathParam("id") Long sample_id) {
        Result<AutoCloseableIterator<DNAExtract>> iter = mgx.getDNAExtractDAO().bySample(sample_id);
        if (iter.isError()) {
            throw new MGXWebException(iter.getError());
        }
        return DNAExtractDTOFactory.getInstance().toDTOList(iter.getValue());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        Result<DNAExtract> obj = mgx.getDNAExtractDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }

        UUID taskId;
        try {
            Result<TaskI> delete = mgx.getDNAExtractDAO().delete(id);
            if (delete.isError()) {
                throw new MGXWebException(delete.getError());
            }
            taskId = taskHolder.addTask(delete.getValue());
        } catch (IOException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }
}
