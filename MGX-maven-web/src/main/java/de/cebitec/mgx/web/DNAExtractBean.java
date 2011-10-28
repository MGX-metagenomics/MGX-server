package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.DNAExtractDTOList;
import de.cebitec.mgx.dto.DNAExtractDTOList.Builder;
import de.cebitec.mgx.dto.MGXLong;
import de.cebitec.mgx.dtoadapter.DNAExtractDTOFactory;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
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
@Path("DNAExtract")
public class DNAExtractBean {

    @Inject
    @MGX
    MGXController mgx;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong create(DNAExtractDTO dto) {
        Long DNAExtract_id = null;
        try {
            Sample s = mgx.getSampleDAO().getById(dto.getSampleId());
            DNAExtract d = DNAExtractDTOFactory.getInstance().toDB(dto);
            s.addDNAExtract(d);
            DNAExtract_id = mgx.getDNAExtractDAO().create(d);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXLong.newBuilder().setValue(DNAExtract_id).build();
    }

    @POST
    @Path("update")
    @Consumes("application/x-protobuf")
    public Response update(DNAExtractDTO dto) {
        DNAExtract h = DNAExtractDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getDNAExtractDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTO fetch(@PathParam("id") Long id) {
        if (id == null) {
            throw new MGXWebException("No ID supplied");
        }
        DNAExtract obj = null;
        try {
            obj = mgx.getDNAExtractDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        if (obj == null) {
            throw new MGXWebException("No such DNAExtract");
        }
        return DNAExtractDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList fetchall() {
        Builder b = DNAExtractDTOList.newBuilder();
        for (DNAExtract o : mgx.getDNAExtractDAO().getAll()) {
            b.addExtract(DNAExtractDTOFactory.getInstance().toDTO(o));
        }
        return b.build();
    }

    @GET
    @Path("bySample/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList bySample(@PathParam("id") Long sample_id) {
        Sample s = null;
        try {
            s = mgx.getSampleDAO().getById(sample_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        Builder b = DNAExtractDTOList.newBuilder();
        for (DNAExtract o : mgx.getDNAExtractDAO().bySample(s)) {
            b.addExtract(DNAExtractDTOFactory.getInstance().toDTO(o));
        }
        return b.build();
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {
        if (id == null) {
            throw new MGXWebException("No ID supplied");
        }
        try {
            mgx.getDNAExtractDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
}
