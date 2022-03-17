package de.cebitec.mgx.web;

import com.google.protobuf.ByteString;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.FileDownloadProvider;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.BytesDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.MappedSequenceDTOList;
import de.cebitec.mgx.dto.dto.MappingDTO;
import de.cebitec.mgx.dto.dto.MappingDTOList;
import de.cebitec.mgx.dtoadapter.MappedSequenceDTOFactory;
import de.cebitec.mgx.dtoadapter.MappingDTOFactory;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.sessions.MappingDataSession;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.model.misc.MappedSequence;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.UUID;

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
    MappingSessions mapSessions;
    @EJB
    DownloadSessions dsessions;

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
        try {
            return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().getAll());
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
    }

    @GET
    @Path("bySeqRun/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList bySeqRun(@PathParam("id") Long run_id) {
        try {
            return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().bySeqRun(run_id));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("byReference/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList byReference(@PathParam("id") Long ref_id) {
        try {
            return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().byReference(ref_id));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("byJob/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList byJob(@PathParam("id") Long job_id) {
        try {
            return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().byJob(job_id));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    // access to mapping details, session-based
    @GET
    @Path("openMapping/{mapid}")
    @Produces("application/x-protobuf")
    public MGXString openMapping(@PathParam("mapid") Long mapid) {
        UUID uuid = null;
        try {
            Mapping m = mgx.getMappingDAO().getById(mapid);
            Reference ref = mgx.getReferenceDAO().getById(m.getReferenceId());
            uuid = mapSessions.addSession(new MappingDataSession(mapid, m.getReferenceId(), ref.getLength(), mgx.getProjectName(), new File(m.getBAMFile())));
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
            iter = session.get(from, to);
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
        try {
            MappingDataSession session = mapSessions.getSession(uuid);
            maxCov = session.getMaxCoverage();
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXLong.newBuilder().setValue(maxCov).build();
    }

    @GET
    @Path("getGenomicCoverage/{uuid}")
    @Produces("application/x-protobuf")
    public MGXLong getGenomicCoverage(@PathParam("uuid") UUID uuid) {
        long maxCov = -1;
        try {
            MappingDataSession session = mapSessions.getSession(uuid);
            maxCov = session.getGenomicCoverage();
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXLong.newBuilder().setValue(maxCov).build();
    }

    @GET
    @Path("closeMapping/{uuid}")
    @Produces("application/x-protobuf")
    public Response closeMapping(@PathParam("uuid") UUID uuid) {
        try {
            mapSessions.removeSession(uuid);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    /*
     * file download interface
     */
    @GET
    @Path("initDownload/{id}")
    @Produces("application/x-protobuf")
    public MGXString initDownload(@PathParam("id") Long id) {

        Mapping obj;
        try {
            obj = mgx.getMappingDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        File target = new File(obj.getBAMFile());
        mgx.log("initiating BAM file download for " + target.getAbsolutePath());
        FileDownloadProvider fdp = new FileDownloadProvider(mgx.getProjectName(), target);

        UUID uuid = dsessions.registerDownloadSession(fdp);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("get/{uuid}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public BytesDTO get(@PathParam("uuid") UUID session_id) {
        try {
            DownloadProviderI<byte[]> dp = dsessions.<byte[]>getSession(session_id);
            return dto.BytesDTO.newBuilder().setData(ByteString.copyFrom(dp.fetch())).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
            dsessions.closeSession(session_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }
}
