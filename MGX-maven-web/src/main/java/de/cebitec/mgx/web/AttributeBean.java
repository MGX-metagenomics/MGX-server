package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.AttributeCount;
import de.cebitec.mgx.dto.AttributeDTO;
import de.cebitec.mgx.dto.AttributeDTOList;
import de.cebitec.mgx.dto.AttributeDTOList.Builder;
import de.cebitec.mgx.dto.AttributeDistribution;
import de.cebitec.mgx.util.Triple;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
    @Path("listTypes")
    @Produces("application/x-protobuf")
    public AttributeDTOList listTypes() {
        Builder ret = AttributeDTOList.newBuilder();
        Set<String> types = mgx.getAttributeDAO().listTypes();
        for (String type : types) {
            AttributeDTO dto = AttributeDTO.newBuilder().setType(type).build();
            ret.addAttribute(dto);
        }

        return ret.build();
    }

    @GET
    @Path("listTypesByJob/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeDTOList listTypesByJob(@PathParam("jobId") Long jobId) {

        Builder ret = AttributeDTOList.newBuilder();
        Set<String> listTypesByJob = mgx.getAttributeDAO().listTypesByJob(jobId);
        for (String type :  listTypesByJob) {
            AttributeDTO dto = AttributeDTO.newBuilder().setType(type).build();
            ret.addAttribute(dto);
        }

        return ret.build();
    }

    @GET
    @Path("getDistribution/{attrName}/{jobId}/{runIDs}")
    @Produces("application/x-protobuf")
    public AttributeDistribution getDistribution(@PathParam("attrName") String attrName, @PathParam("jobId") Long jobId, @PathParam("runIDs") String seqrun_id_list) {

        List<Long> seqrun_ids = new ArrayList<Long>();
        for (String s : split(seqrun_id_list, ",")) {
            seqrun_ids.add(Long.parseLong(s));
        }

        Map<Triple<Long, String, Long>, Long> dist;
        try {
            dist = mgx.getAttributeDAO().getDistribution(attrName, jobId, seqrun_ids);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        AttributeDistribution.Builder b = AttributeDistribution.newBuilder();
        Iterator<Triple<Long, String, Long>> it = dist.keySet().iterator();
        while (it.hasNext()) {
            Triple<Long, String, Long> attr = it.next();
            Long count = dist.get(attr);
            //mgx.log("adding " + attr.getSecond() + "/" + count);
            AttributeDTO dto = AttributeDTO.newBuilder().setId(attr.getFirst()).setType(attr.getSecond()).setParentId(attr.getThird()).build();
            AttributeCount attrcnt = AttributeCount.newBuilder().setAttribute(dto).setCount(count).build();
            b.addAttributecount(attrcnt);
        }

        return b.build();
    }

    private static List<String> split(String message, String separator) {
        return new ArrayList<String>(Arrays.asList(message.split(separator)));
    }
}
