package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.MappingDTO;
import de.cebitec.mgx.dto.dto.MappingDTOList;
import de.cebitec.mgx.dtoadapter.MappingDTOFactory;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Mapping")
public class MappingBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public MappingDTO fetch(@PathParam("id") Long id) {
        Mapping obj;
        try {
            obj = mgx.getMappingDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException("No such Sample");
        }
        return MappingDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public MappingDTOList fetchall() {
        return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().getAll());
    }

    @GET
    @Path("bySeqRun/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList bySeqRun(@PathParam("id") Long run_id) {
        SeqRun run;
        try {
            run = mgx.getSeqRunDAO().getById(run_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        AutoCloseableIterator<Mapping> mappings = mgx.getMappingDAO().bySeqRun(run);
        return MappingDTOFactory.getInstance().toDTOList(mappings);
    }

    @GET
    @Path("byReference/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList byReference(@PathParam("id") Long ref_id) {
        Reference ref;
        try {
            ref = mgx.getReferenceDAO().getById(ref_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        AutoCloseableIterator<Mapping> mappings = mgx.getMappingDAO().byReference(ref);
        return MappingDTOFactory.getInstance().toDTOList(mappings);
    }

    // access to mapping details, session-based
    @GET
    @Path("openMapping/{mapid}")
    @Produces("application/x-protobuf")
    public MGXString openMapping(@PathParam("mapid") Long mapid) {
        UUID uuid = null;
        try {
            Mapping m = mgx.getMappingDAO().getById(mapid);
        } catch (MGXException ex) {
            Logger.getLogger(MappingBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("byReferenceInterval/{uuid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public void byReferenceInterval(@PathParam("uuid") UUID uuid, @PathParam("from") int from, @PathParam("to") int to) {
    }

    @GET
    @Path("closeMapping/{uuidid}")
    @Produces("application/x-protobuf")
    public Response closeMapping(@PathParam("uuid") UUID uuid) {
        taskHolder.removeTask(uuid);
        return Response.ok().build();
    }
}
