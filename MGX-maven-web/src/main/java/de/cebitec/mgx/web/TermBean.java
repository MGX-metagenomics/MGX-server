package de.cebitec.mgx.web;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.dtoadapter.TermDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.db.Term;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.EJB;
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
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    @EJB
    MGXGlobal global;

    @GET
    @Path("byCategory/{term}")
    @Produces("application/x-protobuf")
    public TermDTOList byCategory(@PathParam("term") String cat) {
        return TermDTOFactory.getInstance().toDTOList(global.getTermDAO().byCategory(cat));
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public TermDTO fetch(@PathParam("id") Long id) {
        Term obj = null;
        try {
            obj = global.getTermDAO().getById(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return TermDTOFactory.getInstance().toDTO(obj);
    }
}
