package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.AttributeCorrelation;
import de.cebitec.mgx.dto.dto.AttributeCorrelation.Builder;
import de.cebitec.mgx.dto.dto.AttributeCount;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDistribution;
import de.cebitec.mgx.dto.dto.CorrelatedAttributeCount;
import de.cebitec.mgx.dto.dto.SearchRequestDTO;
import de.cebitec.mgx.dto.dto.SearchResultDTOList;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.util.Pair;
import de.cebitec.mgx.util.SearchResult;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Attribute")
public class AttributeBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("getDistribution/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getDistribution(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        Map<Attribute, Long> dist;
        try {
            //AttributeType attrType = mgx.getAttributeTypeDAO().getById(attrTypeId);
            Job job = mgx.getJobDAO().getById(jobId);
            assert (job != null && job.getStatus() == JobState.FINISHED);
            dist = mgx.getAttributeDAO().getDistribution(attrTypeId, job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return convert(dist);
    }

    @GET
    @Path("getHierarchy/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getHierarchy(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {

        Map<Attribute, Long> dist;
        try {
            Job job = mgx.getJobDAO().getById(jobId);
            assert (job != null && job.getStatus() == JobState.FINISHED);
            dist = mgx.getAttributeDAO().getHierarchy(attrTypeId, job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return convert(dist);
    }

    @GET
    @Path("getCorrelation/{attrTypeId}/{jobId}/{attrType2Id}/{job2Id}")
    @Produces("application/x-protobuf")
    public AttributeCorrelation getCorrelation(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId, @PathParam("attrType2Id") Long attrType2Id, @PathParam("job2Id") Long job2Id) {

        Map<Pair<Attribute, Attribute>, Long> ret;
        try {
            Job job = mgx.getJobDAO().getById(jobId);
            assert (job != null && job.getStatus() == JobState.FINISHED);
            Job job2 = mgx.getJobDAO().getById(job2Id);
            assert (job2 != null && job2.getStatus() == JobState.FINISHED);
            
            ret = mgx.getAttributeDAO().getCorrelation(attrTypeId, job, attrType2Id, job2);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return convertCorrelation(ret);
    }
    
    @PUT
    @Path("search")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public SearchResultDTOList search(SearchRequestDTO req) {
        try {
            Collection<SearchResult> search = mgx.getAttributeDAO().search(req.getTerm(), req.getExact(), req.getSeqrunIdList());
        } catch (MGXException ex) {
            Logger.getLogger(AttributeBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private AttributeDistribution convert(Map<Attribute, Long> dist) {

        Set<AttributeType> aTypes = new HashSet<>();

        AttributeDistribution.Builder b = AttributeDistribution.newBuilder();
        Iterator<Attribute> it = dist.keySet().iterator();
        while (it.hasNext()) {
            Attribute attr = it.next();
            AttributeDTO attrDTO = AttributeDTOFactory.getInstance().toDTO(attr);
            Long count = dist.get(attr);
            AttributeCount attrcnt = AttributeCount.newBuilder().setAttribute(attrDTO).setCount(count).build();

            aTypes.add(attr.getAttributeType());

            b.addAttributeCounts(attrcnt);
        }

        for (AttributeType at : aTypes) {
            b.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(at));
        }

        return b.build();
    }

    private AttributeCorrelation convertCorrelation(Map<Pair<Attribute, Attribute>, Long> ret) {
        
        Set<AttributeType> aTypes = new HashSet<>();
        Builder ac = AttributeCorrelation.newBuilder();
        
        for (Entry<Pair<Attribute, Attribute>, Long> e : ret.entrySet()) {
            Attribute first = e.getKey().getFirst();
            Attribute second = e.getKey().getSecond();
            Long count = e.getValue();
            
            CorrelatedAttributeCount cac = CorrelatedAttributeCount.newBuilder()
                .setRestrictedAttribute(AttributeDTOFactory.getInstance().toDTO(first))
                .setAttribute(AttributeDTOFactory.getInstance().toDTO(second))
                .setCount(count)
                .build();
            ac.addEntry(cac);
            
            aTypes.add(first.getAttributeType());
            aTypes.add(second.getAttributeType());
        }
        
        for (AttributeType at : aTypes) {
            ac.addAttributeType(AttributeTypeDTOFactory.getInstance().toDTO(at));
        }
        return ac.build();
    }
}
