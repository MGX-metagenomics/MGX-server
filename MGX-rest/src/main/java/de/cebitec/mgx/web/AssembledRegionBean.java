package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.GeneByAttributeDownloadProvider;
import de.cebitec.mgx.dto.dto.AssembledRegionDTOList;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.BinSearchResultDTOList;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AssembledRegionDTOFactory;
import de.cebitec.mgx.dtoadapter.BinSearchResultDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.AssembledRegion;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.misc.BinSearchResult;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
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
@Path("AssembledRegion")
@Stateless
public class AssembledRegionBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    DownloadSessions downSessions;
    @EJB
    TaskHolder taskHolder;
    @EJB
    Executor executor;

//    @PUT
//    @Path("create")
//    @Consumes("application/x-protobuf")
//    @Produces("application/x-protobuf")
//    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
//    public MGXLong create(ReferenceRegionDTO dto) {
//        ReferenceRegion x = ReferenceRegionDTOFactory.getInstance().toDB(dto);
//        try {
//            long id = mgx.getReferenceRegionDAO().create(x);
//            return MGXLong.newBuilder().setValue(id).build();
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//    }
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
//    @GET
//    @Path("fetch/{id}")
//    @Produces("application/x-protobuf")
//    public ReferenceRegionDTO fetch(@PathParam("id") Long id) {
//        ReferenceRegion obj = null;
//        try {
//            obj = mgx.getReferenceRegionDAO().getById(id);
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return ReferenceRegionDTOFactory.getInstance().toDTO(obj);
//    }
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
//    @GET
//    @Path("byReferenceInterval/{refid}/{from}/{to}")
//    @Produces("application/x-protobuf")
//    public ReferenceRegionDTOList byReferenceInterval(@PathParam("refid") Long refid, @PathParam("from") int from, @PathParam("to") int to) {
//        ReferenceRegionDTOList ret = null;
//        try {
//            Reference ref = mgx.getReferenceDAO().getById(refid);
//            ret = ReferenceRegionDTOFactory.getInstance().toDTOList(mgx.getReferenceRegionDAO().byReferenceInterval(refid, ref, from, to));
//        } catch (MGXException ex) {
//            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
//        }
//        return ret;
//    }
    @GET
    @Path("byBin/{id}")
    @Produces("application/x-protobuf")
    public AssembledRegionDTOList byBin(@PathParam("id") Long id) {
        Result<AutoCloseableIterator<AssembledRegion>> iter = mgx.getAssembledRegionDAO().byBin(id);
        if (iter.isError()) {
            throw new MGXWebException(iter.getError());
        }
        return AssembledRegionDTOFactory.getInstance().toDTOList(iter.getValue());
    }

    @GET
    @Path("byContig/{id}")
    @Produces("application/x-protobuf")
    public AssembledRegionDTOList byContig(@PathParam("id") Long id) {
        Result<AutoCloseableIterator<AssembledRegion>> iter = mgx.getAssembledRegionDAO().byContig(id);
        if (iter.isError()) {
            throw new MGXWebException(iter.getError());
        }
        return AssembledRegionDTOFactory.getInstance().toDTOList(iter.getValue());
    }

    @GET
    @Path("getDNASequence/{id}")
    @Produces("application/x-protobuf")
    public SequenceDTO getDNASequence(@PathParam("id") Long id) {
        Result<Sequence> obj = mgx.getAssembledRegionDAO().getDNASequence(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return SequenceDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("search/{id}/{term}")
    @Produces("application/x-protobuf")
    public BinSearchResultDTOList search(@PathParam("id") Long bin_id, @PathParam("term") String term) {
        Result<AutoCloseableIterator<BinSearchResult>> iter = mgx.getAssembledRegionDAO().search(bin_id, term);
        if (iter.isError()) {
            throw new MGXWebException(iter.getError());
        }
        return BinSearchResultDTOFactory.getInstance().toDTOList(iter.getValue());
    }

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
        } catch (IOException ex) {
            mgx.log(ex);
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
