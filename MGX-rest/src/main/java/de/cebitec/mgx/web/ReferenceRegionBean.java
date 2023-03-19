package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.GeneByAttributeDownloadProvider;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTO;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.ReferenceRegionDTOFactory;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.ReferenceRegion;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 *
 * @author sjaenick
 */
@Path("ReferenceRegion")
@Stateless
public class ReferenceRegionBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    DownloadSessions downSessions;
    @EJB
    TaskHolder taskHolder;
    @EJB
    Executor executor;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(ReferenceRegionDTO dto) {
        ReferenceRegion x = ReferenceRegionDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getReferenceRegionDAO().create(x);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

//    @POST
//    @Path("update")
//    @Consumes("application/x-protobuf")
//    @Produces("application/x-protobuf")
//    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
//    public Response update(ReferenceRegionDTO dto) {
//        ReferenceRegion h = ReferenceRegionDTOFactory.getInstance().toDB(dto);
//        try {
//            mgx.getReferenceRegionDAO().update(h);
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return Response.ok().build();
//    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public ReferenceRegionDTO fetch(@PathParam("id") Long id) {
        ReferenceRegion obj = null;
        try {
            obj = mgx.getReferenceRegionDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ReferenceRegionDTOFactory.getInstance().toDTO(obj);
    }

//    @GET
//    @Path("fetchall")
//    @Produces("application/x-protobuf")
//    public ReferenceRegionDTOList fetchall() {
//        try {
//            return ReferenceRegionDTOFactory.getInstance().toDTOList(mgx.getReferenceRegionDAO().getAll());
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//    }

    @GET
    @Path("byReferenceInterval/{refid}/{from}/{to}")
    @Produces("application/x-protobuf")
    public ReferenceRegionDTOList byReferenceInterval(@PathParam("refid") Long refid, @PathParam("from") int from, @PathParam("to") int to) {
        ReferenceRegionDTOList ret = null;
        try {
            Reference ref = mgx.getReferenceDAO().getById(refid);
            ret = ReferenceRegionDTOFactory.getInstance().toDTOList(mgx.getReferenceRegionDAO().byReferenceInterval(refid, ref, from, to));
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return ret;
    }

//    @GET
//    @Path("byBin/{id}")
//    @Produces("application/x-protobuf")
//    public GeneDTOList byBin(@PathParam("id") Long id) {
//        AutoCloseableIterator<Gene> bins;
//        try {
//            bins = mgx.getGeneDAO().byBin(id);
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return RegionDTOFactory.getInstance().toDTOList(bins);
//    }
//
//    @GET
//    @Path("byContig/{id}")
//    @Produces("application/x-protobuf")
//    public GeneDTOList byContig(@PathParam("id") Long id) {
//        AutoCloseableIterator<Gene> bins;
//        try {
//            bins = mgx.getGeneDAO().byContig(id);
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return RegionDTOFactory.getInstance().toDTOList(bins);
//    }
//    @GET
//    @Path("getDNASequence/{id}")
//    @Produces("application/x-protobuf")
//    public SequenceDTO getDNASequence(@PathParam("id") Long id) {
//        Sequence obj;
//        try {
//            obj = mgx.getReferenceRegionDAO().getDNASequence(id);
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return SequenceDTOFactory.getInstance().toDTO(obj);
//    }

//    @DELETE
//    @Path("delete/{id}")
//    @Produces("application/x-protobuf")
//    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
//    public MGXString delete(@PathParam("id") Long id) {
//        UUID taskId;
//        try {
//            taskId = taskHolder.addTask(mgx.getReferenceRegionDAO().delete(id));
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return MGXString.newBuilder().setValue(taskId.toString()).build();
//
//    }

    @PUT
    @Path("initDownloadforAttributes")
    @Produces("application/x-protobuf")
    public MGXString initDownloadforAttributes(AttributeDTOList attrdtos) {

        if (attrdtos.getAttributeCount() == 0) {
            throw new MGXWebException("No attributes provided.");
        }

        GeneByAttributeDownloadProvider provider = null;
        try {
            long[] attributeIDs = new long[attrdtos.getAttributeCount()];
            int i = 0;
            for (AttributeDTO dto : attrdtos.getAttributeList()) {
                attributeIDs[i++] = dto.getId();
            }

            mgx.log("Creating attribute-based gene download session for " + mgx.getProjectName());

            provider = new GeneByAttributeDownloadProvider(mgx.getDataSource(), mgx.getProjectName(),
                    attributeIDs, mgx.getProjectAssemblyDirectory());
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
            downSessions.closeSession(session_id);
            mgx.log("Download finished for " + mgx.getProjectName());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetchSequences/{uuid}")
    @Consumes("application/x-protobuf")
    public SequenceDTOList fetchSequences(@PathParam("uuid") UUID session_id) {
        try {
            DownloadProviderI<SequenceDTOList> session = downSessions.getSession(session_id);
            SequenceDTOList ret = session.fetch();
            if (session instanceof Runnable) {
                // submit for async background prefetch
                executor.execute((Runnable) session);
            }
            return ret;
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
    }
}
