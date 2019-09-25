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
import de.cebitec.mgx.dto.dto.AssemblyDTO;
import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.BinDTOList;
import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.ContigDTOList;
import de.cebitec.mgx.dto.dto.GeneCoverageDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.GeneDTOList;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AssemblyDTOFactory;
import de.cebitec.mgx.dtoadapter.BinDTOFactory;
import de.cebitec.mgx.dtoadapter.ContigDTOFactory;
import de.cebitec.mgx.dtoadapter.GeneDTOFactory;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
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
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.UnixHelper;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    @EJB
    Executor executor;
    private static final Logger LOG = Logger.getLogger(ServiceBean.class.getName());

    @GET
    @Path("getJob")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public JobDTO getJob(@HeaderParam("apiKey") String apiKey) {
        try {
            Job job = mgx.getJobDAO().getByApiKey(apiKey);
            return JobDTOFactory.getInstance().toDTO(job);
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @GET
    @Path("fetchSeqRun/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public SeqRunDTO fetchSeqRun(@HeaderParam("apiKey") String apiKey, @PathParam("id") Long id) {
        SeqRun seqrun;
        try {
            Job job = mgx.getJobDAO().getByApiKey(apiKey);
            boolean ok = false;
            for (long l : job.getSeqrunIds()) {
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
            provider = new SeqRunDownloadProvider(mgx.getDataSource(), mgx.getProjectName(), dbFile, 3_000);
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXServiceException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public SequenceDTOList fetchSequences(@HeaderParam("apiKey") String apiKey, @PathParam("uuid") UUID session_id) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            DownloadProviderI<SequenceDTOList> session = downSessions.getSession(session_id);
            SequenceDTOList ret = session.fetch();
            if (session instanceof Runnable) {
                // submit for async background prefetch
                executor.execute((Runnable) session);
            }
            return ret;
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
            x.setAsmjobId(asmJob.getId());
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

    @GET
    @Path("getAssembly")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public AssemblyDTO getAssembly(@HeaderParam("apiKey") String apiKey) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            Assembly asm = mgx.getAssemblyDAO().byJob(asmJob.getId());
            return AssemblyDTOFactory.getInstance().toDTO(asm);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @GET
    @Path("getBins")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public BinDTOList getBins(@HeaderParam("apiKey") String apiKey) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            Assembly asm = mgx.getAssemblyDAO().getById(asmJob.getAssemblyId());
            AutoCloseableIterator<Bin> iter = mgx.getBinDAO().byAssembly(asm.getId());
            return BinDTOFactory.getInstance().toDTOList(iter);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @GET
    @Path("getContigs/{bin_id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public ContigDTOList getContigs(@HeaderParam("apiKey") String apiKey, @PathParam("bin_id") long bin_id) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            AutoCloseableIterator<Contig> iter = mgx.getContigDAO().byBin(bin_id);
            return ContigDTOFactory.getInstance().toDTOList(iter);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @GET
    @Path("getGenes/{contig_ids}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public GeneDTOList getGenes(@HeaderParam("apiKey") String apiKey, @PathParam("contig_ids") String contig_ids) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            String[] splitted = contig_ids.split(",");
            Collection<Long> ids = new ArrayList<>(splitted.length);
            for (String s : splitted) {
                ids.add(Long.valueOf(s));
            }
            AutoCloseableIterator<Gene> iter = mgx.getGeneDAO().byContigs(ids);
            return GeneDTOFactory.getInstance().toDTOList(iter);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @GET
    @Path("getSequence/{contig_ids}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public SequenceDTOList getSequence(@HeaderParam("apiKey") String apiKey, @PathParam("contig_ids") String contig_ids) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);

            SequenceDTOList.Builder ret = SequenceDTOList.newBuilder();

            String[] splitted = contig_ids.split(",");
            Bin bin = null;
            File assemblyDir = null;
            File binFasta = null;
            IndexedFastaSequenceFile ifsf = null;
            for (String ctg_id : splitted) {

                Contig contig = mgx.getContigDAO().getById(Long.valueOf(ctg_id));

                if (bin == null || bin.getId() != contig.getBinId()) {
                    bin = mgx.getBinDAO().getById(contig.getBinId());
                    assemblyDir = new File(mgx.getProjectAssemblyDirectory(), String.valueOf(bin.getAssemblyId()));
                    if (binFasta == null || !binFasta.equals(new File(assemblyDir, String.valueOf(bin.getId()) + ".fna"))) {
                        binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
                        if (ifsf != null) {
                            ifsf.close();
                        }
                        ifsf = new IndexedFastaSequenceFile(binFasta);
                    }
                }

                String contigSeq;
                ReferenceSequence seq;
                seq = ifsf.getSequence(contig.getName());
                if (seq == null || seq.length() == 0) {
                    throw new MGXServiceException("No sequence found for contig " + contig.getName());
                }
                contigSeq = new String(seq.getBases());
                ret.addSeq(SequenceDTO.newBuilder()
                        .setId(contig.getId())
                        .setName(contig.getName())
                        .setSequence(contigSeq)
                        .build());
            }

            if (ifsf != null) {
                ifsf.close();
            }

            return ret.build();
        } catch (MGXException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createContigs")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLongList createContigs(@HeaderParam("apiKey") String apiKey, ContigDTOList dto) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);

            MGXLongList.Builder ret = MGXLongList.newBuilder();
            for (ContigDTO contig : dto.getContigList()) {
                Contig c = ContigDTOFactory.getInstance().toDB(contig);
                long id = mgx.getContigDAO().create(c);
                ret.addLong(id);
            }
            return ret.build();
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("appendSequences/{binId}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response appendSequences(@HeaderParam("apiKey") String apiKey, @PathParam("binId") Long binId, SequenceDTOList dto) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            Bin bin = mgx.getBinDAO().getById(binId);
            File asmRoot = mgx.getProjectAssemblyDirectory();
            File assemblyDir = new File(asmRoot, String.valueOf(bin.getAssemblyId()));
            if (!assemblyDir.exists()) {
                assemblyDir.mkdirs();
            }
            if (!UnixHelper.isGroupWritable(assemblyDir)) {
                UnixHelper.makeDirectoryGroupWritable(assemblyDir.getAbsolutePath());
            }
            File binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
            if (!binFasta.exists()) {
                UnixHelper.createFile(binFasta);
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(binFasta, true))) {

                for (SequenceDTO seq : dto.getSeqList()) {
                    Sequence s = SequenceDTOFactory.getInstance().toDB(seq);
                    bw.write(">");
                    bw.write(s.getName());
                    bw.newLine();
                    bw.write(s.getSequence());
                    bw.newLine();
                }

            }
        } catch (MGXException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @PUT
    @Path("createGenes")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLongList createGenes(@HeaderParam("apiKey") String apiKey, GeneDTOList dtos) {
        try {
            Job asmJob = mgx.getJobDAO().getByApiKey(apiKey);
            MGXLongList.Builder generatedIds = MGXLongList.newBuilder();
            for (GeneDTO dto : dtos.getGeneList()) {
                Gene c = GeneDTOFactory.getInstance().toDB(dto);
                long id = mgx.getGeneDAO().create(c);
                generatedIds.addLong(id);
            }
            return generatedIds.build();
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
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
            LOG.log(Level.SEVERE, null, ex);
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
