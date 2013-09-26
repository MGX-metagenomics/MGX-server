package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MappingDTO;
import de.cebitec.mgx.dto.dto.MappingDTOList;
import de.cebitec.mgx.dto.dto.SampleDTOList;
import de.cebitec.mgx.dtoadapter.MappingDTOFactory;
import de.cebitec.mgx.dtoadapter.SampleDTOFactory;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
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
@Path("Mapping")
public class MappingBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public MappingDTO fetch(@PathParam("id") Long id) {
        Mapping obj;
        try {
            obj = mgx.getMappingDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException("No such Sample");
        }
        return MappingDTOFactory.getInstance().toDTO(obj);
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public MappingDTOList fetchall() {
        return MappingDTOFactory.getInstance().toDTOList(mgx.getMappingDAO().getAll());
    }

    @GET
    @Path("bySeqRun/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList bySeqRun(@PathParam("id") Long run_id) {
        SeqRun run;
        try {
            run = mgx.getSeqRunDAO().getById(run_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        AutoCloseableIterator<Mapping> mappings = mgx.getMappingDAO().bySeqRun(run);
        return MappingDTOFactory.getInstance().toDTOList(mappings);
    }

    @GET
    @Path("byReference/{id}")
    @Produces("application/x-protobuf")
    public MappingDTOList byReference(@PathParam("id") Long ref_id) {
        Reference ref;
        try {
            ref = mgx.getReferenceDAO().getById(ref_id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        AutoCloseableIterator<Mapping> mappings = mgx.getMappingDAO().byReference(ref);
        return MappingDTOFactory.getInstance().toDTOList(mappings);
    }
}
