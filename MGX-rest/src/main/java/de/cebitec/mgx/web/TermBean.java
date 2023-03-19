package de.cebitec.mgx.web;

import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.dtoadapter.TermDTOFactory;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.global.model.Term;
import de.cebitec.mgx.util.AutoCloseableIterator;
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
        Result<AutoCloseableIterator<Term>> res = global.getTermDAO().byCategory(cat);
        if (res.isError()) {
            throw new MGXWebException(ExceptionMessageConverter.convert(res.getError()));
        }
        return TermDTOFactory.getInstance().toDTOList(res.getValue());
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public TermDTO fetch(@PathParam("id") Long id) {
        Result<Term> obj = global.getTermDAO().getById(id);
        if (obj.isError()) {
            throw new MGXWebException(ExceptionMessageConverter.convert(obj.getError()));
        }
        return TermDTOFactory.getInstance().toDTO(obj.getValue());
    }
}
