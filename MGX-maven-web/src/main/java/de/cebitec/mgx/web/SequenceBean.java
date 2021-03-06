package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.SeqByAttributeDownloadProvider;
import de.cebitec.mgx.download.SeqRunDownloadProvider;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.upload.SeqUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Sequence")
public class SequenceBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    UploadSessions upSessions;
    @EJB
    DownloadSessions downSessions;
    @EJB
    Executor executor;

    /*
     * 
     * Upload interface
     * 
     * 
     */
    @GET
    @Path("initUpload/{id}/{hasQual}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString initUpload(@PathParam("id") Long seqrun_id, @PathParam("hasQual") Boolean hasQual) {
        UUID uuid = null;
        try {
            // check seqrun exists before creating upload session
            mgx.getSeqRunDAO().getById(seqrun_id);

            mgx.log("Creating upload session for " + mgx.getProjectName());
            SeqUploadReceiver recv = new SeqUploadReceiver(executor, mgx.getProjectDirectory(),
                    mgx.getProjectQCDirectory(), mgx.getDataSource(), mgx.getProjectName(), seqrun_id,
                    hasQual);
            uuid = upSessions.registerUploadSession(recv);

        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    /*
     * backward compability for old clients
     */
    @GET
    @Path("initUpload/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString initUpload(@PathParam("id") Long seqrun_id) {
        return initUpload(seqrun_id, false);
    }

    @GET
    @Path("closeUpload/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response closeUpload(@PathParam("uuid") UUID session_id) {
        try {
            upSessions.closeSession(session_id);
            mgx.log("Upload finished for " + mgx.getProjectName());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @POST
    @Path("add/{uuid}")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response add(@PathParam("uuid") UUID session_id, SequenceDTOList seqList) {
        try {
            upSessions.getSession(session_id).add(seqList);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("cancelUpload/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response cancelUpload(@PathParam("uuid") UUID session_id) {
        try {
            upSessions.cancelSession(session_id);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    /*
     * 
     * Download interface
     * 
     * 
     */
    @GET
    @Path("initDownload/{id}")
    @Produces("application/x-protobuf")
    public MGXString initDownload(@PathParam("id") Long seqrun_id) {
        SeqRunDownloadProvider provider = null;
        try {
            // make sure requested run exists
            SeqRun seqrun = mgx.getSeqRunDAO().getById(seqrun_id);
            mgx.log("Creating download session for run ID " + seqrun_id);
            provider = new SeqRunDownloadProvider(mgx.getDataSource(), mgx.getProjectName(), seqrun.getDBFile());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        // submit for async background prefetch
        executor.execute(provider);

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @PUT
    @Path("initDownloadforAttributes/")
    @Produces("application/x-protobuf")
    public MGXString initDownloadforAttributes(AttributeDTOList attrdtos) {

        if (attrdtos.getAttributeCount() == 0) {
            throw new MGXWebException("No attributes provided.");
        }

        SeqByAttributeDownloadProvider provider = null;
        try {
            long[] attributeIDs = new long[attrdtos.getAttributeCount()];
            int i = 0;
            for (AttributeDTO dto : attrdtos.getAttributeList()) {
                attributeIDs[i++] = dto.getId();
            }

            mgx.log("Creating attribute-based download session for " + mgx.getProjectName());

            List<Job> jobs = mgx.getJobDAO().byAttributes(attributeIDs);

            //
            // make sure all jobs refer to the same seqrun
            //
            SeqRun seqrun = mgx.getSeqRunDAO().getById(jobs.get(0).getSeqrunId());
            for (Job job : jobs) {
                if (seqrun.getId() != job.getSeqrunId()) {
                    throw new MGXException("Selected attributes refer to different sequencing runs.");
                }
            }

            provider = new SeqByAttributeDownloadProvider(mgx.getDataSource(), mgx.getProjectName(), attributeIDs, seqrun.getDBFile());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        // submit for async background prefetch
        executor.execute(provider);

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
            if (ret.getComplete() == false) {
                // submit for async background prefetch
                executor.execute((Runnable) session);
            }
            return ret;
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
    }

    @GET
    @Path("cancelDownload/{uuid}")
    public Response cancelDownload(@PathParam("uuid") UUID session_id) {
        try {
            downSessions.cancelSession(session_id);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }
        return Response.ok().build();
    }

    /*
     * 
     * retrieval of individual sequences
     * 
     */
    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public SequenceDTO fetch(@PathParam("id") Long id) {
        Sequence obj;
        try {
            obj = mgx.getSequenceDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SequenceDTOFactory.getInstance().toDTO(obj);
    }

    //
    // this should be @GET, but GF has problems with encoded slashes (%2F)
    //
    @PUT
    @Path("byName/{runId}")
    @Produces("application/x-protobuf")
    public SequenceDTO byName(@PathParam("runId") Long runId, MGXString seqName) {
        Sequence obj;
        try {
            obj = mgx.getSequenceDAO().byName(runId, seqName.getValue());
        } catch (MGXException ex) {
            mgx.log(ex.toString());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SequenceDTOFactory.getInstance().toDTO(obj);
    }

    @PUT
    @Path("fetchByIds")
    @Produces("application/x-protobuf")
    public SequenceDTOList fetchByIds(MGXLongList ids) {
        AutoCloseableIterator<Sequence> objs;
        try {
            objs = mgx.getSequenceDAO().getByIds(ids.getLongList());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SequenceDTOFactory.getInstance().toDTOList(objs);
    }

    @GET
    @Path("fetchSequenceIDs/{attrId}")
    @Produces("application/x-protobuf")
    public MGXLongList fetchSequenceIDs(@PathParam("attrId") Long attrId) {
        try (AutoCloseableIterator<Long> lIter = mgx.getSequenceDAO().getSeqIDs(attrId)) {
            MGXLongList.Builder b = MGXLongList.newBuilder();
            while (lIter.hasNext()) {
                b.addLong(lIter.next());
            }
            return b.build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

//    private void createDirs() throws IOException {
//        StringBuilder dir = new StringBuilder(mgx.getProjectDirectory().getAbsolutePath())
//                .append(File.separator)
//                .append("seqruns");
//        File f = new File(dir.toString());
//        if (!f.exists()) {
//            UnixHelper.createDirectory(f);
//        }
//        if (!UnixHelper.isGroupWritable(f)) {
//            UnixHelper.makeDirectoryGroupWritable(f.getAbsolutePath());
//        }
//
//        f = mgx.getProjectQCDirectory();
//        if (!f.exists()) {
//            UnixHelper.createDirectory(f);
//        }
//        if (!UnixHelper.isGroupWritable(f)) {
//            UnixHelper.makeDirectoryGroupWritable(f.getAbsolutePath());
//        }
//    }
}
