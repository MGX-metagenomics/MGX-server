package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dto.dto.JobAndAttributeTypes;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobsAndAttributeTypesDTO;
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
import de.cebitec.mgx.model.dao.workers.DeleteSeqRun;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Term;
import de.cebitec.mgx.qc.QCResult;
import de.cebitec.mgx.qc.io.Loader;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
@Path("SeqRun")
public class SeqRunBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;
    @EJB
    MGXConfiguration mgxconfig;
    @EJB
    MGXGlobal global;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(SeqRunDTO dto) {
        DNAExtract extract;
        long run_id;
        try {
            extract = mgx.getDNAExtractDAO().getById(dto.getExtractId());
            SeqRun seqrun = SeqRunDTOFactory.getInstance(global).toDB(dto);
            seqrun.setExtract(extract);
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
        SeqRun orig = null;
        Term seqMethod = null;
        Term seqTech = null;
        try {
            orig = mgx.getSeqRunDAO().getById(dto.getId());
            seqMethod = global.getTermDAO().getById(dto.getSequencingMethod().getId());
            seqTech = global.getTermDAO().getById(dto.getSequencingTechnology().getId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        orig.setSubmittedToINSDC(dto.getSubmittedToInsdc())
                .setSequencingMethod(seqMethod.getId())
                .setSequencingTechnology(seqTech.getId())
                .setName(dto.getName());

        if (dto.getSubmittedToInsdc()) {
            orig.setAccession(dto.getAccession());
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
        SeqRun seqrun;

        try {
            seqrun = mgx.getSeqRunDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return SeqRunDTOFactory.getInstance(global).toDTO(seqrun);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public SeqRunDTOList fetchall() {
        return SeqRunDTOFactory.getInstance(global).toDTOList(mgx.getSeqRunDAO().getAll());
    }

    @GET
    @Path("byExtract/{id}")
    @Produces("application/x-protobuf")
    public SeqRunDTOList byExtract(@PathParam("id") Long extract_id) {
        DNAExtract extract = null;
        try {
            extract = mgx.getDNAExtractDAO().getById(extract_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return SeqRunDTOFactory.getInstance(global).toDTOList(mgx.getSeqRunDAO().byDNAExtract(extract));
    }

    @DELETE
    @Path("delete/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {
        UUID taskId = taskHolder.addTask(new DeleteSeqRun(id, mgx.getConnection(), mgx.getProjectName(), mgx.getProjectDirectory()));
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("getQC/{id}")
    @Produces("application/x-protobuf")
    public QCResultDTOList getQC(@PathParam("id") Long id) {
        File qcDir = new File(mgx.getProjectDirectory() + "QC");
        List<QCResult> qcList = new ArrayList<>();
        File[] listFiles = qcDir.listFiles();
        if (listFiles != null) {
            for (File f : listFiles) {
                if (f.getName().startsWith(String.valueOf(id) + ".")) {
                    try {
                        QCResult qcr = Loader.load(f.getCanonicalPath());
                        qcList.add(qcr);
                    } catch (IOException ex) {
                        throw new MGXWebException(ex.getMessage());
                    }
                }
            }
        }
        Collections.sort(qcList);
        return QCResultDTOFactory.getInstance().toDTOList(new ForwardingIterator<>(qcList.iterator()));
    }

    @GET
    @Path("JobsAndAttributeTypes/{seqrun_id}")
    @Produces("application/x-protobuf")
    public JobsAndAttributeTypesDTO getJobsAndAttributeTypes(@PathParam("seqrun_id") Long seqrun_id) {

        // TODO - too many DB roundtrips here
        SeqRun run;
        try {
            run = mgx.getSeqRunDAO().getById(seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        JobsAndAttributeTypesDTO.Builder b = JobsAndAttributeTypesDTO.newBuilder();
        try (AutoCloseableIterator<Job> iter = mgx.getJobDAO().BySeqRun(run)) {
            while (iter.hasNext()) {
                Job job = iter.next();
                JobDTO jobDTO = JobDTOFactory.getInstance().toDTO(job);

                try (AutoCloseableIterator<AttributeType> atiter = mgx.getAttributeTypeDAO().ByJob(job.getId())) {
                    AttributeTypeDTOList dtoList = AttributeTypeDTOFactory.getInstance().toDTOList(atiter);
                    JobAndAttributeTypes jat = JobAndAttributeTypes.newBuilder()
                            .setJob(jobDTO).setAttributeTypes(dtoList)
                            .build();
                    b.addEntry(jat);
                }

            }
        } catch (Exception ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return b.build();
    }
}
