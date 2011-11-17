package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXBoolean;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.jobsubmitter.MGXInsufficientJobConfigurationException;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.dto.dto.JobDTOList.Builder;
import de.cebitec.mgx.jobsubmitter.JobSubmitter;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.web.exception.MGXJobException;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import javax.ejb.EJB;
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
@Path("Job")
public class JobBean {

    @Inject
    @MGX
    MGXController mgx;
    @EJB
    JobSubmitter js;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXLong create(JobDTO dto) {
        Tool tool = null;
        SeqRun seqrun = null;
        try {
            tool = mgx.getToolDAO().getById(dto.getToolId());
            seqrun = mgx.getSeqRunDAO().getById(dto.getSeqrunId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        Job j = JobDTOFactory.getInstance().toDB(dto);

        // we artificially set the ID to 'null'; otherwise, JPA considers this
        // object an detached entity passed to persist()..
        j.setId(null);

        j.setStatus(JobState.CREATED);
        j.setTool(tool);
        j.setSeqrun(seqrun);
        j.setParameters("");
        j.setCreator(mgx.getCurrentUser());

        Long job_id = null;
        try {
            job_id = mgx.getJobDAO().create(j);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXLong.newBuilder().setValue(job_id).build();
    }

    @GET
    @Path("fetch/{id}")
    @Produces("application/x-protobuf")
    public JobDTO fetch(@PathParam("id") Long id) {
        try {
            Job job = mgx.getJobDAO().getById(id);
            return JobDTOFactory.getInstance().toDTO(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public JobDTOList fetchall() {
        Builder b = JobDTOList.newBuilder();
        for (Object o : mgx.getJobDAO().getAll()) {
            b.addJob(JobDTOFactory.getInstance().toDTO((Job) o));
        }
        return b.build();
    }

    @GET
    @Path("verify/{id}")
    @Produces("application/x-protobuf")
    public MGXBoolean verify(@PathParam("id") Long id) {
        boolean verified = false;

        try {
            verified = js.verify(mgx, id);
        } catch (MGXInsufficientJobConfigurationException ex) {
            throw new MGXJobException(ex.getMessage());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXBoolean.newBuilder().setValue(verified).build();
    }

    @GET
    @Path("execute/{id}")
    @Produces("application/x-protobuf")
    public MGXBoolean execute(@PathParam("id") Long id) {

        boolean submitted = false;
        try {
            submitted = js.submit(mgx, id);
        } catch (MGXInsufficientJobConfigurationException ex) {
            throw new MGXJobException(ex.getMessage());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        } catch (MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXBoolean.newBuilder().setValue(submitted).build();
    }

    @GET
    @Path("cancel/{id}")
    @Produces("application/x-protobuf")
    public MGXBoolean cancel(@PathParam("id") Long id) {

        boolean ret = false;
        try {
            ret = js.cancel(mgx, id);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        } catch (MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXBoolean.newBuilder().setValue(ret).build();
    }

    @POST
    @Path("update")
    public Response update(JobDTO dto) {
        Job h = JobDTOFactory.getInstance().toDB(dto);
        try {
            mgx.getJobDAO().update(h);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("delete/{id}")
    public Response delete(@PathParam("id") Long id) {

        // remove persistent files
        mgx.getJobDAO().delete(id);

        // notify dispatcher to delete observations
        try {
            js.delete(mgx, id);
        } catch (Exception ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return Response.ok().build();
    }
}
