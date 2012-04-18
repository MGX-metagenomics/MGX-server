package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SeqRunDTOList;
import de.cebitec.mgx.dto.dto.SeqRunDTOList.Builder;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.SeqRunDTOFactory;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.HashMap;
import java.util.Map;
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

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong create(SeqRunDTO dto) {
        DNAExtract extract = null;
        Long SeqRun_id = null;
        try {
            extract = mgx.getDNAExtractDAO().getById(dto.getExtractId());
            SeqRun seqrun = SeqRunDTOFactory.getInstance().toDB(dto);
            seqrun.setExtract(extract);
            SeqRun_id = mgx.getSeqRunDAO().create(seqrun);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(SeqRun_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    public Response update(SeqRunDTO dto) {
        /*
         * since not all fields are exposed via the DTO, we need to fetch
         * the original object from the backend and update it's fields
         * 
         */
        SeqRun orig = null;
        try {
            orig = mgx.getSeqRunDAO().getById(dto.getId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        orig.setSubmittedToINSDC(dto.getSubmittedToInsdc())
            .setSequencingMethod(dto.getSequencingMethod())
            .setSequencingTechnology(dto.getSequencingTechnology());
        
        if (dto.getSubmittedToInsdc())
            orig.setAccession(dto.getAccession());
        
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
        return SeqRunDTOFactory.getInstance().toDTO(seqrun);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public SeqRunDTOList fetchall() {
        return SeqRunDTOFactory.getInstance().toDTOList(mgx.getSeqRunDAO().getAll());
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
        return SeqRunDTOFactory.getInstance().toDTOList(mgx.getSeqRunDAO().byDNAExtract(extract));
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getSeqRunDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
    
    
    @GET
    @Path("JobsAndAttributeTypes/{seqrun_id}")
    @Produces("application/x-protobuf")
    public dto.JobsAndAttributeTypesDTO getJobsAndAttributeTypes(@PathParam("seqrun_id") Long seqrun_id) {
        SeqRun run;
        try {
            run = mgx.getSeqRunDAO().getById(seqrun_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        Map<Job, Iterable<AttributeType>> result = new HashMap<Job, Iterable<AttributeType>>();
        for (Job job : mgx.getJobDAO().BySeqRun(run)) {
            result.put(job, mgx.getAttributeTypeDAO().ByJob(job.getId()));
        }
        
        // convert to DTO - FIXME: move to correct package
        dto.JobsAndAttributeTypesDTO.Builder b = dto.JobsAndAttributeTypesDTO.newBuilder();
        for (Map.Entry<Job, Iterable<AttributeType>> entry : result.entrySet()) {
            dto.JobDTO toDTO = JobDTOFactory.getInstance().toDTO(entry.getKey());
            dto.AttributeTypeDTOList dtoList = AttributeTypeDTOFactory.getInstance().toDTOList(entry.getValue());
            dto.JobAndAttributeTypes jat = dto.JobAndAttributeTypes.newBuilder()
                    .setJob(toDTO)
                    .setAttributeTypes(dtoList)
                    .build();
            b.addEntry(jat);
        }
        return b.build();
    }
}
