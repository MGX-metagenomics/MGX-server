package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXRoles;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.dto.dto.JobParameterDTO;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("JobParameter")
public class JobParameterBean {

    @Inject
    @MGX
    MGXController mgx;

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public JobParameterDTO fetch(@PathParam("id") Long id) {
        Result<JobParameter> jobparam = mgx.getJobParameterDAO().getById(id);
        if (jobparam.isError()) {
            throw new MGXWebException(jobparam.getError());
        }
        return JobParameterDTOFactory.getInstance().toDTO(jobparam.getValue());
    }

    @POST
    @Path("update")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response update(JobParameterDTO dto) {
        JobParameter h = JobParameterDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getJobParameterDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("delete/{id}")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public Response delete(@PathParam("id") Long id) {
        try {
            mgx.getJobParameterDAO().delete(id);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @GET
    @Path("ByJob/{job_id}")
    @Produces("application/x-protobuf")
    public JobParameterListDTO ByJob(@PathParam("job_id") Long job_id) {
        Result<AutoCloseableIterator<JobParameter>> byJob = mgx.getJobParameterDAO().byJob(job_id);
        if (byJob.isError()) {
            throw new MGXWebException(byJob.getError());
        }
        return JobParameterDTOFactory.getInstance().toDTOList(byJob.getValue());
    }
}
