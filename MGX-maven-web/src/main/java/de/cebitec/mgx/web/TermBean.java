package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.dtoadapter.TermDTOFactory;
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
@Path("Term")
@Stateless
public class TermBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("byCategory/{term}")
    @Produces("application/x-protobuf")
    public TermDTOList byCategory(@PathParam("term") String cat) {
        return TermDTOFactory.getInstance().toDTOList(mgx.getGlobal().getTermDAO().byCategory(cat));
    }
}
