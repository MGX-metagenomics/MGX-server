package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.dto.DNAExtractDTOList;
import de.cebitec.mgx.dto.dto.MGXLong;
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
public class DNAExtractBean implements CRUD<DNAExtractDTO, DNAExtractDTOList> {

    @Inject
    @MGX
    MGXController mgx;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Override
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
    @Override
    public Response update(DNAExtractDTO dto) {
        Sample s = null;
        try {
            s = mgx.getSampleDAO().getById(dto.getSampleId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        DNAExtract extract = DNAExtractDTOFactory.getInstance().toDB(dto);
        extract.setSample(s);
        try {
            mgx.getDNAExtractDAO().update(extract);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    @Override
    public DNAExtractDTO fetch(@PathParam("id") Long id) {
        DNAExtract obj = null;
        try {
            obj = mgx.getDNAExtractDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return DNAExtractDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    @Override
    public DNAExtractDTOList fetchall() {
        return DNAExtractDTOFactory.getInstance().toDTOList(mgx.getDNAExtractDAO().getAll());
    }

    @GET
    @Path("bySample/{id}")
    @Produces("application/x-protobuf")
    public DNAExtractDTOList bySample(@PathParam("id") Long sample_id) {
        Sample sample;
        try {
            sample = mgx.getSampleDAO().getById(sample_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return DNAExtractDTOFactory.getInstance().toDTOList(mgx.getDNAExtractDAO().bySample(sample));
    }

    @DELETE
    @Path("delete/{id}")
    @Override
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getDNAExtractDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }
}
