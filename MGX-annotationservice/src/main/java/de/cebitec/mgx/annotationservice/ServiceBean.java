package de.cebitec.mgx.annotationservice;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.annotationservice.exception.MGXServiceException;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.SeqRunDownloadProvider;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.AssemblyDTO;
import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AssemblyDTOFactory;
import de.cebitec.mgx.dtoadapter.BinDTOFactory;
import de.cebitec.mgx.dtoadapter.ContigDTOFactory;
import de.cebitec.mgx.dtoadapter.GeneDTOFactory;
import de.cebitec.mgx.dtoadapter.SeqRunDTOFactory;
import de.cebitec.mgx.dtoadapter.SequenceDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Sequence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Path("AnnotationService")
@Stateless
public class ServiceBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    MGXGlobal global;
    @EJB
    DownloadSessions downSessions;

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTO fetch(@HeaderParam("apiKey") String apiKey, @PathParam("id") Long id) {
        SeqRun seqrun;
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            boolean ok = false;
            for (long l : asmJob.getSeqrunIds()) {
                if (id == l) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new MGXServiceException("Invalid API key.");
            }
            seqrun = mgx.getSeqRunDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }

        return SeqRunDTOFactory.getInstance(global).toDTO(seqrun);
    }

    @GET
    @Path("initDownload/{id}")
    @Produces("application/x-protobuf")
    public MGXString initDownload(@HeaderParam("apiKey") String apiKey, @PathParam("id") Long seqrun_id) {
        SeqRunDownloadProvider provider = null;
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            boolean ok = false;
            for (long l : asmJob.getSeqrunIds()) {
                if (seqrun_id == l) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new MGXServiceException("Invalid API key.");
            }
            mgx.log("Creating download session for run ID " + seqrun_id);
            String dbFile = mgx.getSeqRunDAO().getDBFile(seqrun_id).getAbsolutePath();
            provider = new SeqRunDownloadProvider(mgx.getDataSource(), mgx.getProjectName(), dbFile);
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXServiceException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return dto.MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    public Response closeDownload(@HeaderParam("apiKey") String apiKey, @PathParam("uuid") UUID session_id) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            downSessions.closeSession(session_id);
            mgx.log("Download finished for " + mgx.getProjectName());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetchSequences/{uuid}")
    @Consumes("application/x-protobuf")
    public dto.SequenceDTOList fetchSequences(@HeaderParam("apiKey") String apiKey, @PathParam("uuid") UUID session_id) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            DownloadProviderI<SequenceDTOList> session = downSessions.getSession(session_id);
            return session.fetch();
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createAssembly")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createAssembly(@HeaderParam("apiKey") String apiKey, AssemblyDTO dto) {
        Assembly x = AssemblyDTOFactory.getInstance().toDB(dto);
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            long id = mgx.getAssemblyDAO().create(x);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createBin")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createBin(@HeaderParam("apiKey") String apiKey, BinDTO dto) {
        Bin bin = BinDTOFactory.getInstance().toDB(dto);
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            long id = mgx.getBinDAO().create(bin);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createContig")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createContig(@HeaderParam("apiKey") String apiKey, ContigDTO dto) {
        Contig c = ContigDTOFactory.getInstance().toDB(dto);
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            long id = mgx.getContigDAO().create(c);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("appendSequence/{binId}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response appendSequence(@HeaderParam("apiKey") String apiKey, @PathParam("binId") Long binId, SequenceDTO dto) {
        Sequence s = SequenceDTOFactory.getInstance().toDB(dto);
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);

            Bin bin = mgx.getBinDAO().getById(binId);
            File asmRoot = mgx.getProjectAssemblyDirectory();
            File assemblyDir = new File(asmRoot, String.valueOf(bin.getAssemblyId()));
            if (!assemblyDir.exists()) {
                assemblyDir.mkdirs();
            }
            File binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(binFasta, true))) {
                bw.write(">");
                bw.write(s.getName());
                bw.newLine();
                bw.write(s.getSequence());
                bw.newLine();
            }
        } catch (MGXException | IOException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @PUT
    @Path("createGene")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createGene(@HeaderParam("apiKey") String apiKey, GeneDTO dto) {
        Gene c = GeneDTOFactory.getInstance().toDB(dto);
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            long id = mgx.getGeneDAO().create(c);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createGeneCoverage")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response createGeneCoverage(@HeaderParam("apiKey") String apiKey, GeneCoverageDTOList dtoList) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            for (GeneCoverageDTO geneCov : dtoList.getGeneCoverageList()) {
                if (!arrayContains(asmJob.getSeqrunIds(), geneCov.getRunId())) {
                    throw new MGXServiceException("Invalid seqrun ID for gene " + geneCov.getGeneId());
                }
                mgx.getGeneDAO().createCoverage(geneCov.getGeneId(), geneCov.getRunId(), geneCov.getCoverage());
            }
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("finishJob")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response finishJob(@HeaderParam("apiKey") String apiKey) {
        try {
            Job j = mgx.getJobDAO().getByApiKey(apiKey);
            j.setStatus(JobState.FINISHED);
            mgx.getJobDAO().update(j);
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    private static boolean arrayContains(long[] arr, long val) {
        for (long l : arr) {
            if (l == val) {
                return true;
            }
        }
        return false;
    }
}
