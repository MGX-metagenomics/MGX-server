package de.cebitec.mgx.web;

import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.dtoadapter.TermDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.MGXGlobalException;
import de.cebitec.mgx.global.model.Term;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 *
 * @author sjaenick
 */
@Path("Term")
@Stateless
public class TermBean {

    @EJB
    MGXGlobal global;

    @GET
    @Path("byCategory/{term}")
    @Produces("application/x-protobuf")
    public TermDTOList byCategory(@PathParam("term") String cat) {
        try {
            return TermDTOFactory.getInstance().toDTOList(global.getTermDAO().byCategory(cat));
        } catch (MGXGlobalException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public TermDTO fetch(@PathParam("id") Long id) {
        Term obj = null;
        try {
            obj = global.getTermDAO().getById(id);
        } catch (MGXGlobalException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return TermDTOFactory.getInstance().toDTO(obj);
    }
}
