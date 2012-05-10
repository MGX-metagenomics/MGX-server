package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.AttributeCount;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDistribution;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
            assert(job != null && job.getStatus() == JobState.FINISHED);
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
            assert(job != null && job.getStatus() == JobState.FINISHED);
            dist = mgx.getAttributeDAO().getHierarchy(attrTypeId, job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        
        return convert(dist);
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
            
            //
            b.addAttributeCounts(attrcnt);
        }
        
        for (AttributeType at : aTypes) {
            b.addAttributeTypes(AttributeTypeDTOFactory.getInstance().toDTO(at));
        }
        
        return b.build();
    }
}
