package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.AttributeCount;
import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDistribution;
import de.cebitec.mgx.dto.dto.AttributeTypeDTO;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dtoadapter.AttributeDTOFactory;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    @Path("listTypes")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList listTypes() {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().listTypes());
    }

    @GET
    @Path("listTypesByJob/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList listTypesByJob(@PathParam("jobId") Long jobId) {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().listTypesByJob(jobId));
    }

    @GET
    @Path("listTypesBySeqRun/{seqrunId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList listTypesBySeqRun(@PathParam("seqrunId") Long seqrunId) {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().listTypesBySeqRun(seqrunId));
    }

//    @GET
//    @Path("getDistributionByRuns/{attrName}/{runIDs}")
//    @Produces("application/x-protobuf")
//    public AttributeDistribution getDistributionByRuns(@PathParam("attrName") String attrName, @PathParam("runIDs") String seqrun_id_list) {
//        return getDistribution(attrName, null, seqrun_id_list);
//    }
//
    @GET
    @Path("getDistribution/{attrTypeId}/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getDistribution(@PathParam("attrTypeId") Long attrTypeId, @PathParam("jobId") Long jobId) {


        Map<Attribute, Long> dist;
        try {
            AttributeType aType = mgx.getAttributeTypeDAO().getById(attrTypeId);
            dist = mgx.getAttributeDAO().getDistribution(aType, jobId);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        AttributeDistribution.Builder b = AttributeDistribution.newBuilder();
        Iterator<Attribute> it = dist.keySet().iterator();
        while (it.hasNext()) {
            Attribute attr = it.next();
            AttributeDTO attrDTO = AttributeDTOFactory.getInstance().toDTO(attr);
            Long count = dist.get(attr);
            AttributeCount attrcnt = AttributeCount.newBuilder().setAttribute(attrDTO).setCount(count).build();
            //
            b.addAttributecount(attrcnt);
        }

        return b.build();
    }

    private static List<String> split(String message, String separator) {
        return new ArrayList<String>(Arrays.asList(message.split(separator)));
    }
}
