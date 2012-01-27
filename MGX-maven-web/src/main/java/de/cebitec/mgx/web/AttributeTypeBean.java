package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
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
@Path("AttributeType")
public class AttributeTypeBean {

    @Inject
    @MGX
    MGXController mgx;

//    @GET
//    @Path("listTypes")
//    @Produces("application/x-protobuf")
//    public AttributeTypeDTOList listTypes() {
//        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().listTypes());
//    }
//
    @GET
    @Path("ByJob/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList listTypesByJob(@PathParam("jobId") Long jobId) {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().ByJob(jobId));
    }

    @GET
    @Path("BySeqRun/{seqrunId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList BySeqRun(@PathParam("seqrunId") Long seqrunId) {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().BySeqRun(seqrunId));
    }
}
