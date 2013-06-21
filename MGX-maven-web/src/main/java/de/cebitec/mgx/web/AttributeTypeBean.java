package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.AttributeTypeDTO;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
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

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTO fetch(@PathParam("id") Long id) {
        AttributeType obj = null;
        try {
            obj = mgx.getAttributeTypeDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return AttributeTypeDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList fetchall() {
        return AttributeTypeDTOFactory.getInstance().toDTOList(mgx.getAttributeTypeDAO().getAll());
    }

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
