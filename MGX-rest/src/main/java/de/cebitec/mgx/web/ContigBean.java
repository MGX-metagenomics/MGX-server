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
import de.cebitec.mgx.sessions.IteratorHolder;
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
    @EJB
    IteratorHolder iterHolder;

    //
    private final static int FETCH_LIMIT = 20_000;
    //

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
        return extractChunk(all.getValue());
    }

    @GET
    @Path("byBin/{id}")
    @Produces("application/x-protobuf")
    public ContigDTOList byBin(@PathParam("id") Long id) {
        Result<AutoCloseableIterator<Contig>> contigs = mgx.getContigDAO().byBin(id);
        if (contigs.isError()) {
            throw new MGXWebException(contigs.getError());
        }
        return extractChunk(contigs.getValue());
    }

    //
    // extracts up to FETCH_LIMIT contigs from the iterator and returns
    // them; if there are more elements, the iterator is deposited with
    // the iteratorHolder, and the corresponding session UUID is returned
    // with the data chunk
    //
    // subsequent chunks can then be retrieved via continueSession(uuid)
    //
    private ContigDTOList extractChunk(AutoCloseableIterator<Contig> iter) {
        int cnt = 0;
        ContigDTOList.Builder b = ContigDTOList.newBuilder();
        while (iter.hasNext() && cnt < FETCH_LIMIT) {
            ContigDTO dto = ContigDTOFactory.getInstance().toDTO(iter.next());
            b.addContig(dto);
            cnt++;
        }

        // if fetchlimit has been reached, deposit iterator for later
        // retrieval via byBinSession(uuid)
        //
        if (iter.hasNext()) {
            UUID uuid = iterHolder.add(iter);
            b.setComplete(false);
            b.setUuid(uuid.toString());
        } else {
            b.setComplete(true);
            iter.close();
        }

        return b.build();
    }

    @GET
    @Path("continueSession/{uuid}")
    @Produces("application/x-protobuf")
    public ContigDTOList continueSession(@PathParam("uuid") String uuid) {
        UUID session = UUID.fromString(uuid);
        AutoCloseableIterator<Contig> iter = iterHolder.get(session);
        if (iter == null) {
            throw new MGXWebException("Invalid UUID");
        }

        int cnt = 0;
        ContigDTOList.Builder b = ContigDTOList.newBuilder();
        while (iter.hasNext() && cnt < FETCH_LIMIT) {
            ContigDTO dto = ContigDTOFactory.getInstance().toDTO(iter.next());
            b.addContig(dto);
            cnt++;
        }

        if (iter.hasNext()) {
            b.setComplete(false);
            b.setUuid(session.toString());
        } else {
            iterHolder.remove(session);
            b.setComplete(true);
            iter.close();
        }

        return b.build();
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
