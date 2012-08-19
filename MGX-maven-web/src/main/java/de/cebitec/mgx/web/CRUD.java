package de.cebitec.mgx.web;

import de.cebitec.mgx.dto.dto.MGXLong;
import javax.ws.rs.core.Response;

/**
 *
 * @author sj
 */
public interface CRUD<T, U> {

    public MGXLong create(T dto);

    public T fetch(Long id);

    public U fetchall();

    public Response update(T dto);

    public Response delete(Long id);
}
