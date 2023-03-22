package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dtoadapter.GeneCoverageDTOFactory;
import de.cebitec.mgx.model.db.GeneCoverage;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 *
 * @author sjaenick
 */
@Path("GeneCoverage")
@Stateless
public class GeneCoverageBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("byGene/{id}")
    @Produces("application/x-protobuf")
    public GeneCoverageDTOList byGene(@PathParam("id") Long id) {
        Result<AutoCloseableIterator<GeneCoverage>> gcs = mgx.getGeneCoverageDAO().byGene(id);
        if (gcs.isError()) {
            throw new MGXWebException(gcs.getError());
        }
        return GeneCoverageDTOFactory.getInstance().toDTOList(gcs.getValue());
    }
  
}
