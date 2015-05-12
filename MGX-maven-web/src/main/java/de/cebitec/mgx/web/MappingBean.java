package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.MappedSequenceDTOList;
import de.cebitec.mgx.dto.dto.MappingDTO;
import de.cebitec.mgx.dto.dto.MappingDTOList;
import de.cebitec.mgx.dtoadapter.MappedSequenceDTOFactory;
import de.cebitec.mgx.dtoadapter.MappingDTOFactory;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.sessions.MappingDataSession;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.model.misc.MappedSequence;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
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
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MappingSessions")
    MappingSessions mapSessions;

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public MappingDTO fetch(@PathParam("id") Long id) {
        Mapping obj;
        try {
            obj = mgx.getMappingDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
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
            uuid = mapSessions.addSession(new MappingDataSession(m.getReference().getId(), m.getReference().getLength(), mgx.getProjectName(), new File(m.getBAMFile())));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("byReferenceInterval/{uuid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public MappedSequenceDTOList byReferenceInterval(@PathParam("uuid") UUID uuid, @PathParam("from") int from, @PathParam("to") int to) {
        AutoCloseableIterator<MappedSequence> iter;
        try {
            MappingDataSession session = mapSessions.getSession(uuid);
            iter = session.get(mgx.getConnection(), from, to);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MappedSequenceDTOFactory.getInstance().toDTOList(iter);
    }

    @GET
    @Path("getMaxCoverage/{uuid}")
    @Produces("application/x-protobuf")
    public MGXLong getMaxCoverage(@PathParam("uuid") UUID uuid) {
        long maxCov = -1;
        Connection conn = mgx.getConnection();
        try {
            MappingDataSession session = mapSessions.getSession(uuid);
            maxCov = session.getMaxCoverage(conn);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                throw new MGXWebException(ex.getMessage());
            }
        }
        return MGXLong.newBuilder().setValue(maxCov).build();
    }

    @GET
    @Path("closeMapping/{uuid}")
    @Produces("application/x-protobuf")
    public Response closeMapping(@PathParam("uuid") UUID uuid) {
        mapSessions.removeSession(uuid);
        return Response.ok().build();
    }
}
