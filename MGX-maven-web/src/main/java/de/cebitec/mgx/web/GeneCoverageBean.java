package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dtoadapter.GeneCoverageDTOFactory;
import de.cebitec.mgx.model.db.GeneCoverage;
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
        AutoCloseableIterator<GeneCoverage> gcs;
        try {
            gcs = mgx.getGeneCoverageDAO().byGene(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return GeneCoverageDTOFactory.getInstance().toDTOList(gcs);
    }
  
}
