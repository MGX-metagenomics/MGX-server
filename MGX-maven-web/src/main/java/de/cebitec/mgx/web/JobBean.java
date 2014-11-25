package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.MGXBoolean;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.jobsubmitter.JobParameterHelper;
import de.cebitec.mgx.jobsubmitter.JobSubmitter;
import de.cebitec.mgx.jobsubmitter.MGXInsufficientJobConfigurationException;
import de.cebitec.mgx.model.dao.workers.DeleteJob;
import de.cebitec.mgx.model.dao.workers.RestartJob;
import de.cebitec.mgx.model.db.*;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.web.exception.MGXJobException;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import de.cebitec.mgx.web.helper.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
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
    @EJB
    JobParameterHelper paramHelper;
    @EJB
    TaskHolder taskHolder;
    @EJB
    MGXConfiguration mgxconfig;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(JobDTO dto) {

        Job j = JobDTOFactory.getInstance().toDB(dto);
        Set<JobParameter> params = new HashSet<>();
        j.setParameters(params);

        Tool tool = null;
        SeqRun seqrun = null;
        try {
            tool = mgx.getToolDAO().getById(dto.getToolId());
            seqrun = mgx.getSeqRunDAO().getById(dto.getSeqrunId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        // fetch and transfer parameters available for the referenced tool
        try {
            String toolXMLData = Util.readFile(new File(tool.getXMLFile()));
            AutoCloseableIterator<JobParameter> defaultParams = paramHelper.getParameters(toolXMLData);
            while (defaultParams.hasNext()) {
                params.add(defaultParams.next());
            }
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        // we artificially set the ID to 'null'; otherwise, JPA considers this
        // object an detached entity passed to persist()..
        j.setId(null);
        j.setStatus(JobState.CREATED);
        j.setTool(tool);
        j.setSeqrun(seqrun);
        j.setCreator(mgx.getCurrentUser());

        // persist and refetch
        Long job_id = null;
        try {
            job_id = mgx.getJobDAO().create(j);
            j = mgx.getJobDAO().getById(job_id); // refetch
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

//            if (j.getParameters() == null) {
//                j.setParameters(new HashSet<JobParameter>());
//            }
        // update default parameters with those specified by the user
        try {
            Set<JobParameter> userSupplied = JobParameterDTOFactory.getInstance().toDBList(dto.getParameters());

            for (JobParameter defaultParam : j.getParameters()) {
                
                JobParameter userParam = findParameter(defaultParam, userSupplied);
                
                if (userParam == null && !defaultParam.isOptional()) {
                    mgx.getJobDAO().delete(j.getId());
                    throw new MGXWebException("Parameter " + defaultParam.getUserName() + " required but not specified.");
                }
                
                String value = userParam.getParameterValue();

                if (defaultParam.getType().equals("ConfigFile")) {
                    String fullPath = mgx.getProjectDirectory() + "files" + File.separator
                            + userParam.getParameterValue().substring(2).replace("|", File.separator);
                    if (!new File(fullPath).exists()) {
                        throw new MGXWebException("Invalid file path: " + userParam.getParameterValue());
                    }
                    value = fullPath;
                }
                
                defaultParam.setParameterValue(value);
                mgx.getJobParameterDAO().update(defaultParam); // persist changes
            }

        } catch (MGXException ex) {
            try {
                mgx.getJobDAO().delete(j.getId());
            } catch (MGXException ex1) {
                mgx.log(ex1.getMessage());
            }
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
            fixParameters(job);
            return JobDTOFactory.getInstance().toDTO(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("fetchall")
    @Produces("application/x-protobuf")
    public JobDTOList fetchall() {
        List<Job> jobs = new ArrayList<>();
        try {
            AutoCloseableIterator<Job> acit = mgx.getJobDAO().getAll();
            while (acit.hasNext()) {
                Job j = acit.next();
                fixParameters(j);
                jobs.add(j);
            }
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return JobDTOFactory.getInstance().toDTOList(new ForwardingIterator<>(jobs.iterator()));
    }

    @GET
    @Path("getParameters/{id}")
    @Produces("application/x-protobuf")
    public JobParameterListDTO getParameters(@PathParam("id") Long id) {
        try {
            Job job = mgx.getJobDAO().getById(id);
            AutoCloseableIterator<JobParameter> params = mgx.getJobParameterDAO().ByJob(job);
            return JobParameterDTOFactory.getInstance().toDTOList(params);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @POST
    @Path("setParameters/{id}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public void setParameters(@PathParam("id") Long id, JobParameterListDTO paramdtos) {
        try {
            Job job = mgx.getJobDAO().getById(id);
            for (JobParameter jp : JobParameterDTOFactory.getInstance().toDBList(paramdtos)) {
                jp.setJob(job);

                if (jp.getType().equals("ConfigFile")) {
                    String fullPath = mgx.getProjectDirectory() + "files" + File.separator
                            + jp.getParameterValue().substring(2).replace("|", File.separator);
                    jp.setParameterValue(fullPath);
                }
                job.getParameters().add(jp);
            }
            mgx.getJobDAO().update(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("verify/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXBoolean verify(@PathParam("id") Long id) {
        boolean verified = false;

        try {
            Job job = mgx.getJobDAO().getById(id);
            String fname = job.getSeqrun().getDBFile();
            if (fname == null || !new File(fname).exists()) {
                throw new MGXException("Cannot access sequence data for sequencing run.");
            }
            verified = js.validate(mgx, id);
        } catch (MGXInsufficientJobConfigurationException ex) {
            throw new MGXJobException(ex.getMessage());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXBoolean.newBuilder().setValue(verified).build();
    }

    @GET
    @Path("execute/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXBoolean execute(@PathParam("id") Long id) {

        boolean submitted = false;
        try {
            Job job = mgx.getJobDAO().getById(id);
            if (job.getStatus() != JobState.VERIFIED) {
                throw new MGXWebException("Job is in invalid state.");
            }
            submitted = js.submit(mgxconfig.getDispatcherHost(), mgx.getConnection(), mgx.getProjectName(), job);
        } catch (MGXInsufficientJobConfigurationException ex) {
            mgx.log(ex.getMessage());
            throw new MGXJobException(ex.getMessage());
        } catch (MGXException | MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return MGXBoolean.newBuilder().setValue(submitted).build();
    }

    @GET
    @Path("restart/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString restart(@PathParam("id") Long id) {
        Job job;
        try {
            job = mgx.getJobDAO().getById(id);
            RestartJob dJob = new RestartJob(mgx, mgxconfig, job, mgx.getConnection(), mgx.getProjectName(), mgxconfig.getDispatcherHost(), js);
            UUID taskId = taskHolder.addTask(dJob);
            return MGXString.newBuilder().setValue(taskId.toString()).build();
        } catch (MGXException | MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

    }

    @GET
    @Path("cancel/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXBoolean cancel(@PathParam("id") Long id) {
        boolean isActive = true;
        try {
            Job job = mgx.getJobDAO().getById(id);
            JobState status = job.getStatus();
            // check if job has already reached a terminal state
            if (status == JobState.FAILED || status == JobState.FINISHED || status == JobState.ABORTED) {
                isActive = false;
            }
            job.setStatus(JobState.IN_DELETION);
            mgx.getJobDAO().update(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        if (!isActive) {
            throw new MGXWebException("Job is not being processed, cannot cancel.");
        }

        try {
            js.cancel(mgx, id);
        } catch (MGXException | MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXBoolean.newBuilder().setValue(true).build();
    }

    @POST
    @Path("update")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
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
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXString delete(@PathParam("id") Long id) {

        boolean isActive = true;
        try {
            Job job = mgx.getJobDAO().getById(id);
            JobState status = job.getStatus();
            // check if job has already reached a terminal state
            if (status == JobState.FAILED || status == JobState.FINISHED || status == JobState.ABORTED) {
                isActive = false;
            }
            job.setStatus(JobState.IN_DELETION);
            mgx.getJobDAO().update(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        if (isActive) {
            // notify dispatcher
            try {
                js.delete(mgx, id);
            } catch (MGXDispatcherException | MGXException ex) {
                //mgx.log(ex.getMessage());
            }
        }

        // remove persistent files
        try {
            mgx.getJobDAO().delete(id);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        DeleteJob dJob = new DeleteJob(id, mgx.getConnection(), mgx.getProjectName());
        UUID taskId = taskHolder.addTask(dJob);
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("BySeqRun/{seqrun_id}")
    @Produces("application/x-protobuf")
    public JobDTOList BySeqRun(@PathParam("seqrun_id") Long seqrun_id) {
        SeqRun run = null;
        try {
            run = mgx.getSeqRunDAO().getById(seqrun_id);
        } catch (MGXException ex) {
            // we don't fail for non-existing seqruns; instead, an empty
            // result is returned
            return JobDTOFactory.getInstance().toDTOList(mgx.getJobDAO().BySeqRun(run));
        }

        List<Job> jobs = new ArrayList<>();
        try {
            AutoCloseableIterator<Job> acit = mgx.getJobDAO().BySeqRun(run);
            while (acit.hasNext()) {
                Job j = acit.next();
                fixParameters(j);
                jobs.add(j);
            }
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return JobDTOFactory.getInstance().toDTOList(new ForwardingIterator<>(jobs.iterator()));
    }

    @GET
    @Path("GetError/{id}")
    @Produces("application/x-protobuf")
    public MGXString getError(@PathParam("id") Long id) {
        try {
            Job job = mgx.getJobDAO().getById(id);
            return MGXString.newBuilder().setValue(mgx.getJobDAO().getError(job)).build();
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    private void fixParameters(Job job) throws MGXException {
        String toolXMLData = Util.readFile(new File(job.getTool().getXMLFile()));
        List<JobParameter> availableParams = new ArrayList<>();
        AutoCloseableIterator<JobParameter> apIter = paramHelper.getParameters(toolXMLData);
        while (apIter.hasNext()) {
            availableParams.add(apIter.next());
        }

        for (JobParameter jp : job.getParameters()) {
            for (JobParameter candidate : availableParams) {
                // can't compare by ID field here
                if (jp.getNodeId() == candidate.getNodeId() && jp.getParameterName().equals(candidate.getParameterName())) {
                    jp.setClassName(candidate.getClassName());
                    jp.setType(candidate.getType());
                    jp.setDisplayName(candidate.getDisplayName());
                }
            }
        }
    }

    private JobParameter findParameter(JobParameter defaultParam, Set<JobParameter> userSupplied) {
        for (JobParameter jp : userSupplied) {
            if (jp.getNodeId() == defaultParam.getNodeId() && jp.getParameterName().equals(defaultParam.getParameterName())) {
                return jp;
            }
        }
        return null;
    }
}
