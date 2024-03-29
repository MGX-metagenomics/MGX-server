package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.dto.dto.AttributeTypeDTO;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dtoadapter.AttributeTypeDTOFactory;
import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

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

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(AttributeTypeDTO dto) {
        AttributeType h = AttributeTypeDTOFactory.getInstance().toDB(dto);
        try {
            long id = mgx.getAttributeTypeDAO().create(h);
            return MGXLong.newBuilder().setValue(id).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTO fetch(@PathParam("id") Long id) {
        Result<AttributeType> obj = mgx.getAttributeTypeDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return AttributeTypeDTOFactory.getInstance().toDTO(obj.getValue());
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList fetchall() {
        Result<AutoCloseableIterator<AttributeType>> all = mgx.getAttributeTypeDAO().getAll();
        if (all.isError()) {
            throw new MGXWebException(all.getError());
        }
        return AttributeTypeDTOFactory.getInstance().toDTOList(all.getValue());
    }

    @GET
    @Path("ByJob/{jobId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList ByJob(@PathParam("jobId") Long jobId) {
        Result<DBIterator<AttributeType>> obj = mgx.getAttributeTypeDAO().byJob(jobId);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return AttributeTypeDTOFactory.getInstance().toDTOList(obj.getValue());
    }

    @GET
    @Path("BySeqRun/{seqrunId}")
    @Produces("application/x-protobuf")
    public AttributeTypeDTOList BySeqRun(@PathParam("seqrunId") Long seqrunId) {
        Result<DBIterator<AttributeType>> obj = mgx.getAttributeTypeDAO().BySeqRun(seqrunId);
        if (obj.isError()) {
            throw new MGXWebException(obj.getError());
        }
        return AttributeTypeDTOFactory.getInstance().toDTOList(obj.getValue());
    }
}
