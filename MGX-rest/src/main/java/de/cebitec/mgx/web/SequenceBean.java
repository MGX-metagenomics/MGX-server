package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
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
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.sessions.IteratorHolder;
import de.cebitec.mgx.upload.SeqUploadReceiver;
import de.cebitec.mgx.upload.UploadSessions;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

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
    UploadSessions upSessions;
    @EJB
    DownloadSessions downSessions;
    @EJB
    IteratorHolder iterHolder;
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
        // check seqrun exists before creating upload session
        Result<SeqRun> res = mgx.getSeqRunDAO().getById(seqrun_id);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }
        SeqRun run = res.getValue();

        UUID uuid = null;
        try {

            mgx.log("Creating upload session for " + mgx.getProjectName());
            SeqUploadReceiver<? extends DNASequenceI> recv = new SeqUploadReceiver<>(executor, mgx.getProjectDirectory(),
                    mgx.getProjectQCDirectory(), mgx.getDataSource(), mgx.getProjectName(), seqrun_id,
                    hasQual, run.isPaired());
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

        // make sure requested run exists
        Result<SeqRun> res = mgx.getSeqRunDAO().getById(seqrun_id);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }

        SeqRun seqrun = res.getValue();

        SeqRunDownloadProvider provider = null;
        try {
            mgx.log("Creating download session for run ID " + seqrun_id);

            Result<File> dbFile = mgx.getSeqRunDAO().getDBFile(seqrun.getId());
            if (dbFile.isError()) {
                throw new MGXWebException(dbFile.getError());
            }

            provider = new SeqRunDownloadProvider(mgx.getDataSource(),
                    mgx.getProjectName(), dbFile.getValue().getAbsolutePath());
        } catch (IOException | SeqStoreException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        // submit for async background prefetch
        executor.execute(provider);

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @PUT
    @Path("initDownloadforAttributes")
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

            Result<List<Job>> jobres = mgx.getJobDAO().byAttributes(attributeIDs);
            if (jobres.isError()) {
                throw new MGXWebException(jobres.getError());
            }
            List<Job> jobs = jobres.getValue();

            //
            // make sure all jobs refer to the same seqrun
            //
            Result<SeqRun> res = mgx.getSeqRunDAO().getById(jobs.get(0).getSeqrunIds()[0]);
            if (res.isError()) {
                throw new MGXWebException(res.getError());
            }
            SeqRun seqrun = res.getValue();
//            for (Job job : jobs) {
//                long[] seqrunIds = job.getSeqrunIds();
//                if (arrayContains(seqrunIds, seqrun.getId())) {
//                    throw new MGXException("Selected attributes refer to different sequencing runs.");
//                }
//            }
            Result<File> dbFile = mgx.getSeqRunDAO().getDBFile(seqrun.getId());
            if (dbFile.isError()) {
                throw new MGXWebException(dbFile.getError());
            }

            provider = new SeqByAttributeDownloadProvider(mgx.getDataSource(), mgx.getProjectName(),
                    attributeIDs, dbFile.getValue().getAbsolutePath());
        } catch (SeqStoreException | IOException  ex) {
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
        Result<Sequence> res = mgx.getSequenceDAO().getById(id);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }
        return SequenceDTOFactory.getInstance().toDTO(res.getValue());
    }

    //
    // this should be @GET, but GF has problems with encoded slashes (%2F)
    //
    @PUT
    @Path("byName/{runId}")
    @Produces("application/x-protobuf")
    public SequenceDTO byName(@PathParam("runId") Long runId, MGXString seqName) {
        Result<Sequence> obj = mgx.getSequenceDAO().byName(runId, seqName.getValue());
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return SequenceDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @PUT
    @Path("fetchByIds")
    @Produces("application/x-protobuf")
    public SequenceDTOList fetchByIds(MGXLongList ids) {
        Result<AutoCloseableIterator<Sequence>> objs = mgx.getSequenceDAO().getByIds(ids.getLongList());
        if (objs.isError()) {
            throw new MGXWebException(objs.getError());
        }
        return SequenceDTOFactory.getInstance().toDTOList(objs.getValue());
    }

    private final static int FETCH_LIMIT = 50_000;

    @GET
    @Path("fetchSequenceIDs/{attrId}")
    @Produces("application/x-protobuf")
    public MGXLongList fetchSequenceIDs(@PathParam("attrId") Long attrId) {
        Result<AutoCloseableIterator<Long>> res = mgx.getSequenceDAO().getSeqIDs(attrId);
        if (res.isError()) {
            throw new MGXWebException(res.getError());
        }

        AutoCloseableIterator<Long> iter = res.getValue();

        MGXLongList.Builder b = MGXLongList.newBuilder();
        int cnt = 0;
        while (iter.hasNext() && cnt < FETCH_LIMIT) {
            b.addLong(iter.next());
            cnt++;
        }
        
        //
        // we transfer at most FETCH_LIMIT sequences in one chunk; if
        // more data is available, the iterator is deposited within
        // the iterator holder and a session id is provided to the 
        // calling client, which can later retrieve the remaining
        // data.
        //

        if (iter.hasNext()) {
            UUID sessionId = iterHolder.add(iter);
            b.setComplete(false);
            b.setUuid(sessionId.toString());
            // do not close the iterator here, since it will be reused
        } else {
            b.setComplete(true);
            iter.close();
        }
        return b.build();
    }

}
