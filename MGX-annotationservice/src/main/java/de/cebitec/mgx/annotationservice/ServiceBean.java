package de.cebitec.mgx.annotationservice;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.protobuf.ByteString;
import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.annotationservice.exception.MGXServiceException;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.download.DownloadProviderI;
import de.cebitec.mgx.download.DownloadSessions;
import de.cebitec.mgx.download.SeqRunDownloadProvider;
import de.cebitec.mgx.dto.dto.AssembledRegionDTO;
import de.cebitec.mgx.dto.dto.AssembledRegionDTOList;
import de.cebitec.mgx.dto.dto.AssemblyDTO;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeTypeDTO;
import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.BinDTOList;
import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.ContigDTOList;
import de.cebitec.mgx.dto.dto.GeneAnnotationDTOList;
import de.cebitec.mgx.dto.dto.GeneCoverageDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dtoadapter.AssembledRegionDTOFactory;
import de.cebitec.mgx.dtoadapter.AssemblyDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.dtoadapter.BinDTOFactory;
import de.cebitec.mgx.dtoadapter.ContigDTOFactory;
import de.cebitec.mgx.dtoadapter.GeneAnnotationDTOFactory;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.SeqRunDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.db.AssembledRegion;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.model.db.GeneAnnotation;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.seqcompression.FourBitEncoder;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.UnixHelper;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job job = res.getValue();

        return JobDTOFactory.getInstance().toDTO(job);
    }

    @GET
    @Path("fetchSeqRun/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public SeqRunDTO fetchSeqRun(@HeaderParam("apiKey") String apiKey, @PathParam("id") Long id) {
        Result<SeqRun> seqrun = mgx.getSeqRunDAO().getById(id);
        if (seqrun.isError()) {
            throw new MGXServiceException(seqrun.getError());
        }

        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }

        Job job = res.getValue();
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

        return SeqRunDTOFactory.getInstance(global).toDTO(seqrun.getValue());
    }

    @GET
    @Path("initDownload/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString initDownload(@HeaderParam("apiKey") String apiKey, @PathParam("id") Long seqrun_id) {
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job asmJob = res.getValue();

        SeqRunDownloadProvider provider = null;
        try {
            //
            // validate this job contains the requested seqrun id
            //
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

            mgx.log("Creating sequence download session for run ID " + seqrun_id);
            Result<File> dbFile = mgx.getSeqRunDAO().getDBFile(seqrun_id);
            if (dbFile.isError()) {
                throw new MGXServiceException(dbFile.getError());
            }
            provider = new SeqRunDownloadProvider(mgx.getDataSource(),
                    mgx.getProjectName(), dbFile.getValue().getAbsolutePath(), 3_000);

            // submit for async background prefetch
            executor.execute(provider);
            
        } catch (SeqStoreException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXServiceException(ex.getMessage());
        }

        UUID uuid = downSessions.registerDownloadSession(provider);
        return MGXString.newBuilder().setValue(uuid.toString()).build();
    }

    @GET
    @Path("closeDownload/{uuid}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response closeDownload(@PathParam("uuid") UUID session_id) {
        try {
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
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createAssembly")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createAssembly(@HeaderParam("apiKey") String apiKey, AssemblyDTO dto) {
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job asmJob = res.getValue();

        Assembly x = AssemblyDTOFactory.getInstance().toDB(dto);
        x.setAsmjobId(asmJob.getId());

        try {
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
    public MGXLong createBin(BinDTO dto) {
        Bin bin = BinDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getBinDAO().create(bin);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createAttributeType")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createAttributeType(AttributeTypeDTO dto) {
        AttributeType attr = AttributeTypeDTOFactory.getInstance().toDB(dto);

        try {
            long id = mgx.getAttributeTypeDAO().create(attr);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
    }

    @PUT
    @Path("createAttribute")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong createAttribute(AttributeDTO dto) {
        Attribute attr = AttributeDTOFactory.getInstance().toDB(dto);
        attr.setJobId(dto.getJobId());
        attr.setAttributeTypeId(dto.getAttributeTypeId());

        if (dto.getParentId() != 0) {
            attr.setParentId(dto.getParentId());
        }

        try {
            long id = mgx.getAttributeDAO().create(attr);
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
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job asmJob = res.getValue();

        Result<Assembly> asm = mgx.getAssemblyDAO().byJob(asmJob.getId());
        if (asm.isError()) {
            throw new MGXServiceException(asm.getError());
        }
        return AssemblyDTOFactory.getInstance().toDTO(asm.getValue());
    }

    @GET
    @Path("getBins")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public BinDTOList getBins(@HeaderParam("apiKey") String apiKey) {
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job asmJob = res.getValue();

        Result<Assembly> asm = mgx.getAssemblyDAO().getById(asmJob.getAssemblyId());
        if (asm.isError()) {
            throw new MGXServiceException(asm.getError());
        }

        Result<AutoCloseableIterator<Bin>> bins = mgx.getBinDAO().byAssembly(asm.getValue().getId());
        if (bins.isError()) {
            throw new MGXServiceException(bins.getError());
        }
        return BinDTOFactory.getInstance().toDTOList(bins.getValue());
    }

    @GET
    @Path("getContigs/{bin_id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public ContigDTOList getContigs(@PathParam("bin_id") long bin_id) {
        Result<AutoCloseableIterator<Contig>> iter = mgx.getContigDAO().byBin(bin_id);
        if (iter.isError()) {
            throw new MGXServiceException(iter.getError());
        }
        return ContigDTOFactory.getInstance().toDTOList(iter.getValue());
    }

    @GET
    @Path("getSubregions/{contig_ids}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public AssembledRegionDTOList getSubregions(@PathParam("contig_ids") String contig_ids) {
        String[] splitted = contig_ids.split(",");
        Collection<Long> ids = new ArrayList<>(splitted.length);
        for (String s : splitted) {
            ids.add(Long.valueOf(s));
        }

        Result<AutoCloseableIterator<AssembledRegion>> res = mgx.getAssembledRegionDAO().byContigs(ids);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        return AssembledRegionDTOFactory.getInstance().toDTOList(res.getValue());
    }

    private final static LoadingCache<File, IndexedFastaSequenceFile> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<File, IndexedFastaSequenceFile>() {
                @Override
                public void onRemoval(RemovalNotification<File, IndexedFastaSequenceFile> rn) {
                    try {
                        rn.getValue().close();
                    } catch (IOException ex) {
                        Logger.getLogger(ServiceBean.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            })
            .build(new CacheLoader<File, IndexedFastaSequenceFile>() {
                @Override
                public IndexedFastaSequenceFile load(File k) throws Exception {
                    if (!k.exists()) {
                        throw new MGXServiceException("FASTA file missing: " + k.getAbsolutePath());
                    }
                    File idxFile = new File(k.getAbsolutePath() + ".fai");
                    if (!idxFile.exists()) {
                        throw new MGXServiceException("FASTA index file missing: " + idxFile.getAbsolutePath());
                    }
                    return new IndexedFastaSequenceFile(k);
                }
            });

    @GET
    @Path("getSequence/{contig_ids}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public SequenceDTOList getSequence(@PathParam("contig_ids") String contig_ids) {
        try {
            String[] splitted = contig_ids.split(",");
            long[] ids = new long[splitted.length];
            for (int i = 0; i < splitted.length; i++) {
                ids[i] = Long.parseLong(splitted[i]);
            }

            SequenceDTOList.Builder ret = SequenceDTOList.newBuilder();
            Bin bin = null;
            File binFasta = null;
            IndexedFastaSequenceFile ifsf = null;

            Result<AutoCloseableIterator<Contig>> byIds = mgx.getContigDAO().getByIds(ids);
            if (byIds.isError()) {
                throw new MGXServiceException(byIds.getError());
            }

            try ( AutoCloseableIterator<Contig> iter = byIds.getValue()) {

                while (iter != null && iter.hasNext()) {

                    Contig contig = iter.next();

                    if (bin == null || bin.getId() != contig.getBinId()) {
                        Result<Bin> binres = mgx.getBinDAO().getById(contig.getBinId());
                        if (binres.isError()) {
                            throw new MGXServiceException(binres.getError());
                        }
                        bin = binres.getValue();
                        File assemblyDir = new File(mgx.getProjectAssemblyDirectory(), String.valueOf(bin.getAssemblyId()));
                        if (binFasta == null || !binFasta.equals(new File(assemblyDir, String.valueOf(bin.getId()) + ".fna"))) {
                            binFasta = new File(assemblyDir, String.valueOf(bin.getId()) + ".fna");
                            File idxFile = new File(binFasta.getAbsolutePath() + ".fai");
                            if (!idxFile.exists()) {
                                // recreate missing index file
                                Logger.getLogger(ServiceBean.class.getName()).log(Level.SEVERE, "Creating missing FASTA index for {0}", binFasta.getAbsolutePath());
                                mgx.getBinDAO().indexFASTA(binFasta);
                            }
                            try {
                                ifsf = cache.get(binFasta);
                            } catch (ExecutionException ex) {
                                Logger.getLogger(ServiceBean.class.getName()).log(Level.SEVERE, null, ex.getCause());
                            }
                        }
                    }

                    if (ifsf == null) {
                        Logger.getLogger(ServiceBean.class.getName()).log(Level.SEVERE, "Unable to read {0}", binFasta.getAbsolutePath());
                        throw new MGXServiceException("Unable to read " + binFasta.getAbsolutePath());
                    }

                    String contigSeq;
                    ReferenceSequence seq;
                    seq = ifsf.getSequence(contig.getName());
                    if (seq == null || seq.length() == 0) {
                        throw new MGXServiceException("No sequence found for contig " + contig.getName());
                    }

                    // TODO: unneeded string creation here? check this
                    contigSeq = new String(seq.getBases());
                    ret.addSeq(SequenceDTO.newBuilder()
                            .setId(contig.getId())
                            .setName(contig.getName())
                            .setSequence(ByteString.copyFrom(FourBitEncoder.encode(contigSeq.getBytes())))
                            .build());
                }

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
    public MGXLongList createContigs(ContigDTOList dto) {
        try {
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response appendSequences(@PathParam("binId") Long binId, SequenceDTOList dto) {

        Result<Bin> binres = mgx.getBinDAO().getById(binId);
        if (binres.isError()) {
            throw new MGXServiceException(binres.getError());
        }
        Bin bin = binres.getValue();

        try {
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
            try ( BufferedWriter bw = new BufferedWriter(new FileWriter(binFasta, true))) {

                for (SequenceDTO seq : dto.getSeqList()) {
                    bw.write(">");
                    bw.write(seq.getName());
                    bw.newLine();
                    bw.write(new String(FourBitEncoder.decode(seq.getSequence().toByteArray())));
                    bw.newLine();
                }

            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @PUT
    @Path("createSubregions")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLongList createSubregions(AssembledRegionDTOList dtos) {
        try {
            MGXLongList.Builder generatedIds = MGXLongList.newBuilder();
            for (AssembledRegionDTO dto : dtos.getRegionList()) {
                AssembledRegion c = AssembledRegionDTOFactory.getInstance().toDB(dto);
                long id = mgx.getAssembledRegionDAO().create(c);
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
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response createGeneCoverage(@HeaderParam("apiKey") String apiKey, GeneCoverageDTOList dtoList) {
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job asmJob = res.getValue();

        try {
            for (GeneCoverageDTO geneCov : dtoList.getGeneCoverageList()) {
                if (!arrayContains(asmJob.getSeqrunIds(), geneCov.getRunId())) {
                    throw new MGXServiceException("Invalid seqrun ID for gene " + geneCov.getRegionId());
                }
                mgx.getAssembledRegionDAO().createCoverage(geneCov.getRegionId(), geneCov.getRunId(), geneCov.getCoverage());
            }
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @PUT
    @Path("createGeneObservations")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response createGeneObservations(GeneAnnotationDTOList dtoList) {
        try {
            List<GeneAnnotation> annots = GeneAnnotationDTOFactory.getInstance().toList(dtoList);
            mgx.getAssembledRegionDAO().createAnnotations(annots);
        } catch (MGXException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    @GET
    @Path("startJob")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response startJob(@HeaderParam("apiKey") String apiKey) {

        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }

        //
        // upon job start, update state and startdate
        //
        Job job = res.getValue();
        job.setStatus(JobState.RUNNING);

        try {
            mgx.getJobDAO().update(job);
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
        Result<Job> res = mgx.getJobDAO().getByApiKey(apiKey);
        if (res.isError()) {
            throw new MGXServiceException(res.getError());
        }
        Job job = res.getValue();

        Result<Assembly> asm = mgx.getAssemblyDAO().byJob(job.getId());
        if (asm.isError()) {
            throw new MGXServiceException(asm.getError());
        }

        try {
            // update db fields, index FASTA files..
            mgx.getBinDAO().updateDerivedFields(asm.getValue().getId());

            job.setStatus(JobState.FINISHED);
            mgx.getJobDAO().update(job);

        } catch (MGXException ex) {
            throw new MGXServiceException(ex.getMessage());
        }
        return Response.ok().build();
    }

    private static boolean arrayContains(final long[] arr, final long val) {
        for (long l : arr) {
            if (l == val) {
                return true;
            }
        }
        return false;
    }
}
