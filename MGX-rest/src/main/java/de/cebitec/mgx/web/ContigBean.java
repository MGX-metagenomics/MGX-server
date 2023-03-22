package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.ContigDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dtoadapter.ContigDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.Sequence;
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
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
@Path("Contig")
@Stateless
public class ContigBean {

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
    public MGXLong create(ContigDTO dto) {
        Contig x = ContigDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getContigDAO().create(x);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(ContigDTO dto) {
        Contig h = ContigDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getContigDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public ContigDTO fetch(@PathParam("id") Long id) {
        Result<Contig> obj = mgx.getContigDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return ContigDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public ContigDTOList fetchall() {
        Result<AutoCloseableIterator<Contig>> all = mgx.getContigDAO().getAll();
        if (all.isError()) {
            throw new MGXWebException(all.getError());
        }
        return ContigDTOFactory.getInstance().toDTOList(all.getValue());
    }

    @GET
    @Path("byBin/{id}")
    @Produces("application/x-protobuf")
    public ContigDTOList byBin(@PathParam("id") Long id) {
        Result<AutoCloseableIterator<Contig>> contigs = mgx.getContigDAO().byBin(id);
        if (contigs.isError()) {
            throw new MGXWebException(contigs.getError());
        }
        return ContigDTOFactory.getInstance().toDTOList(contigs.getValue());
    }

    @GET
    @Path("getDNASequence/{id}")
    @Produces("application/x-protobuf")
    public SequenceDTO getDNASequence(@PathParam("id") Long id) {
        Result<Sequence> obj = mgx.getContigDAO().getDNASequence(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return SequenceDTOFactory.getInstance().toDTO(obj.getValue());

    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        Result<TaskI> delete = mgx.getContigDAO().delete(id);
        if (delete.isError()) {
            throw new MGXWebException(delete.getError());
        }
        UUID taskId = taskHolder.addTask(delete.getValue());
        return MGXString.newBuilder().setValue(taskId.toString()).build();

    }
}
