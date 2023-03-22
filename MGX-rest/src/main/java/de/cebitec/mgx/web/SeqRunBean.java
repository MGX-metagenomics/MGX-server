package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.common.ToolScope;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dto.dto.JobAndAttributeTypes;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobsAndAttributeTypesDTO;
import de.cebitec.mgx.dto.dto.MGXBoolean;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.QCResultDTOList;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SeqRunDTOList;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.QCResultDTOFactory;
import de.cebitec.mgx.dtoadapter.SeqRunDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.global.model.Term;
import de.cebitec.mgx.qc.Analyzer;
import de.cebitec.mgx.qc.QCFactory;
import de.cebitec.mgx.qc.QCResultI;
import de.cebitec.mgx.qc.io.Loader;
import de.cebitec.mgx.qc.io.Persister;
import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("SeqRun")
public class SeqRunBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;
    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    MGXGlobal global;
    @EJB
    Executor executor;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(SeqRunDTO dto) {
        long run_id;
        try {
            SeqRun seqrun = SeqRunDTOFactory.getInstance(global).toDB(dto);
            run_id = mgx.getSeqRunDAO().create(seqrun);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(run_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(SeqRunDTO dto) {
        /*
         * since not all fields are exposed via the DTO, we need to fetch the
         * original object from the backend and update it's fields
         *
         */
        Result<Term> seqMethod = global.getTermDAO().getById(dto.getSequencingMethod().getId());
        if (seqMethod.isError()) {
            throw new MGXWebException(seqMethod.getError());
        }

        Result<Term> seqTech = global.getTermDAO().getById(dto.getSequencingTechnology().getId());
        if (seqTech.isError()) {
            throw new MGXWebException(seqTech.getError());
        }

        Result<SeqRun> run = mgx.getSeqRunDAO().getById(dto.getId());
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }

        SeqRun orig = run.getValue();
        orig.setSubmittedToINSDC(dto.getSubmittedToInsdc())
                .setSequencingMethod(seqMethod.getValue().getId())
                .setSequencingTechnology(seqTech.getValue().getId())
                .setName(dto.getName())
                .setExtractId(dto.getExtractId());

        if (dto.getSubmittedToInsdc()) {
            orig.setAccession(dto.getAccession());
        } else {
            orig.setAccession("");
        }

        try {
            mgx.getSeqRunDAO().update(orig);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTO fetch(@PathParam("id") Long id) {
        Result<SeqRun> run = mgx.getSeqRunDAO().getById(id);
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }
        return SeqRunDTOFactory.getInstance(global).toDTO(run.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public SeqRunDTOList fetchall() {
        Result<AutoCloseableIterator<SeqRun>> run = mgx.getSeqRunDAO().getAll();
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }
        return SeqRunDTOFactory.getInstance(global).toDTOList(run.getValue());
    }

    @GET
    @Path("byExtract/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTOList byExtract(@PathParam("id") Long extract_id) {
        Result<AutoCloseableIterator<SeqRun>> byDNAExtract = mgx.getSeqRunDAO().byDNAExtract(extract_id);
        if (byDNAExtract.isError()) {
            throw new MGXWebException(byDNAExtract.getError());
        }
        return SeqRunDTOFactory.getInstance(global).toDTOList(byDNAExtract.getValue());
    }

    @GET
    @Path("byJob/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTOList byJob(@PathParam("id") Long jobId) {
        Result<AutoCloseableIterator<SeqRun>> byJob = mgx.getSeqRunDAO().byJob(jobId);
        if (byJob.isError()) {
            throw new MGXWebException(byJob.getError());
        }
        return SeqRunDTOFactory.getInstance(global).toDTOList(byJob.getValue());
    }

    @GET
    @Path("byAssembly/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTOList byAssembly(@PathParam("id") Long asmId) {
        Result<AutoCloseableIterator<SeqRun>> byAssembly = mgx.getSeqRunDAO().byAssembly(asmId);
        if (byAssembly.isError()) {
            throw new MGXWebException(byAssembly.getError());
        }
        return SeqRunDTOFactory.getInstance(global).toDTOList(byAssembly.getValue());
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        Result<SeqRun> run = mgx.getSeqRunDAO().getById(id);
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }

        UUID taskId;
        try {
            Result<TaskI> delete = mgx.getSeqRunDAO().delete(id);
            if (delete.isError()) {
                throw new MGXWebException(delete.getError());
            }
            taskId = taskHolder.addTask(delete.getValue());
        } catch (IOException ex) {
            mgx.log(ex);
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("hasQuality/{id}")
    @Produces("application/x-protobuf")
    public MGXBoolean hasQuality(@PathParam("id") Long id) {
        // make sure run exists
        Result<SeqRun> run = mgx.getSeqRunDAO().getById(id);
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }

        boolean hasQual;
        try {
            hasQual = mgx.getSeqRunDAO().hasQuality(id);
        } catch (IOException | MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXBoolean.newBuilder().setValue(hasQual).build();
    }

    @GET
    @Path("getQC/{id}")
    @Produces("application/x-protobuf")
    @SuppressWarnings("unchecked")
    public QCResultDTOList getQC(@PathParam("id") Long id) {

        Result<SeqRun> run = mgx.getSeqRunDAO().getById(id);
        if (run.isError()) {
            throw new MGXWebException(run.getError());
        }

        SeqRun sr = run.getValue();

        Analyzer<DNASequenceI>[] analyzers = null;
        try {
            if (sr.getNumberOfSequences() > 0) {
                Result<File> dbFile = mgx.getSeqRunDAO().getDBFile(id);
                if (dbFile.isError()) {
                    throw new MGXWebException(dbFile.getError());
                }
                SeqReaderI<? extends DNASequenceI> r = SeqReaderFactory.getReader(dbFile.getValue().getAbsolutePath());
                if (r != null) {
                    analyzers = QCFactory.<DNASequenceI>getQCAnalyzers(r.hasQuality(), sr.isPaired());
                    r.close();
                }
            }
        } catch (IOException | SeqStoreException ex) {
            mgx.log(ex);
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        List<QCResultI> qcList = new ArrayList<>();

        if (analyzers != null && analyzers.length > 0) {
            File qcDir;
            String dbFilename;
            try {
                Result<File> dbFile = mgx.getSeqRunDAO().getDBFile(sr.getId());
                if (dbFile.isError()) {
                    throw new MGXWebException(dbFile.getError());
                }
                dbFilename = dbFile.getValue().getAbsolutePath();
                qcDir = mgx.getProjectQCDirectory();
            } catch (IOException ex) {
                mgx.log(ex);
                throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
            }
            final String prefix = qcDir.getAbsolutePath() + File.separator + id + ".";
            for (final Analyzer<DNASequenceI> analyzer : analyzers) {
                File outFile = new File(prefix + analyzer.getName());

                if (!outFile.exists()) {
                    Logger.getLogger(SeqRunBean.class.getName()).log(Level.SEVERE, "Starting QC analyzer {0} for run {1}", new Object[]{analyzer.getName(), sr.getName()});
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                SeqReaderI<? extends DNASequenceI> r = SeqReaderFactory.<DNASequenceI>getReader(dbFilename);

                                while (r != null && r.hasMoreElements()) {
                                    DNASequenceI h = r.nextElement();
                                    try {
                                        analyzer.<DNASequenceI>add(h);
                                    } catch (SequenceException ex) {
                                        Logger.getLogger(SeqRunBean.class.getName()).log(Level.SEVERE, "Analyzer {0} failed when adding {1}", new Object[]{analyzer.getName(), new String(h.getSequence())});
                                        Logger.getLogger(SeqRunBean.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                if (r != null) {
                                    r.close();
                                }
                                if (analyzer.getNumberOfSequences() == sr.getNumberOfSequences()) {
                                    Persister.persist(prefix, analyzer);
                                } else {
                                    Logger.getLogger(SeqRunBean.class.getName()).log(Level.SEVERE, "Analyzer {0} failed for {1} after {2} seqs", new Object[]{analyzer.getName(), sr.getName(), analyzer.getNumberOfSequences()});
                                }
                            } catch (SequenceException ex) {
                                Logger.getLogger(SeqRunBean.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } else {
                    try {
                        QCResultI qcr = Loader.load(outFile.getCanonicalPath());
                        qcList.add(qcr);
                    } catch (IOException ex) {
                        mgx.log(ex);
                        throw new MGXWebException(ex.getMessage());
                    }
                }
            }
        }
        Collections.sort(qcList);
        return QCResultDTOFactory.getInstance().toDTOList(new ForwardingIterator<>(qcList.iterator()));
    }

    @GET
    @Path("JobsAndAttributeTypes/{seqrun_id}/{assembly_id}/{scope}")
    @Produces("application/x-protobuf")
    public JobsAndAttributeTypesDTO getJobsAndAttributeTypes(@PathParam("seqrun_id") Long seqrun_id, @PathParam("assembly_id") Long assembly_id, @PathParam("scope") int scope) {

        ToolScope tscope = ToolScope.values()[scope];

        JobsAndAttributeTypesDTO.Builder b = JobsAndAttributeTypesDTO.newBuilder();

        switch (tscope) {
            case READ:
                Result<AutoCloseableIterator<Job>> res = mgx.getJobDAO().bySeqRun(seqrun_id);
                if (res.isError()) {
                    throw new MGXWebException(res.getError());
                }

                try ( AutoCloseableIterator<Job> iter = res.getValue()) {
                    while (iter.hasNext()) {
                        Job job = iter.next();
                        JobDTO jobDTO = JobDTOFactory.getInstance().toDTO(job);

                        Result<DBIterator<AttributeType>> res2 = mgx.getAttributeTypeDAO().byJob(job.getId());
                        if (res2.isError()) {
                            throw new MGXWebException(res2.getError());
                        }

                        try ( AutoCloseableIterator<AttributeType> atiter = res2.getValue()) {
                            AttributeTypeDTOList dtoList = AttributeTypeDTOFactory.getInstance().toDTOList(atiter);
                            if (dtoList.getAttributeTypeCount() > 0) {
                                JobAndAttributeTypes jat = JobAndAttributeTypes.newBuilder()
                                        .setJob(jobDTO).setAttributeTypes(dtoList)
                                        .build();
                                b.addEntry(jat);
                            }
                        }
                    }
                }
                break;

            case ASSEMBLY:
                // no-op; assembly jobs do not annotate attribute types
                break;

            case GENE_ANNOTATION:

                Result<AutoCloseableIterator<Job>> byAsm = mgx.getJobDAO().byAssembly(assembly_id);
                if (byAsm.isError()) {
                    throw new MGXWebException(byAsm.getError());
                }
                try ( AutoCloseableIterator<Job> iter = byAsm.getValue()) {
                    while (iter.hasNext()) {
                        Job job = iter.next();
                        JobDTO jobDTO = JobDTOFactory.getInstance().toDTO(job);

                        Result<DBIterator<AttributeType>> byJob = mgx.getAttributeTypeDAO().byJob(job.getId());
                        if (byJob.isError()) {
                            throw new MGXWebException(byJob.getError());
                        }

                        try ( AutoCloseableIterator<AttributeType> atiter = byJob.getValue()) {
                            AttributeTypeDTOList dtoList = AttributeTypeDTOFactory.getInstance().toDTOList(atiter);
                            if (dtoList.getAttributeTypeCount() > 0) {
                                JobAndAttributeTypes jat = JobAndAttributeTypes.newBuilder()
                                        .setJob(jobDTO).setAttributeTypes(dtoList)
                                        .build();
                                b.addEntry(jat);
                            }
                        }
                    }
                }
                break;
        }
        return b.build();
    }
}
