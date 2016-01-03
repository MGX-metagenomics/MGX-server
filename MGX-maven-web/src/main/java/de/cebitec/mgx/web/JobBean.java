package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dispatcher.client.MGXDispatcherConfiguration;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.MGXBoolean;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.dispatcher.common.MGXInsufficientJobConfigurationException;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitter;
import de.cebitec.mgx.workers.DeleteJob;
import de.cebitec.mgx.workers.RestartJob;
import de.cebitec.mgx.model.db.*;
import de.cebitec.mgx.sessions.MappingSessions;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.web.exception.MGXJobException;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    TaskHolder taskHolder;
    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    MGXDispatcherConfiguration dispConfig;
    @EJB
    MappingSessions mappingSessions;

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(JobDTO dto) {

        Job j = JobDTOFactory.getInstance().toDB(dto);

        // we artificially set the IDs to 'null'; otherwise, JPA considers this
        // object an detached entity passed to persist()..
        j.setId(null);
        for (JobParameter jp : j.getParameters()) {
            jp.setId(null);
        }

        Tool tool = null;
        SeqRun seqrun = null;
        try {
            tool = mgx.getToolDAO().getById(dto.getToolId());
            seqrun = mgx.getSeqRunDAO().getById(dto.getSeqrunId());
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        j.setStatus(JobState.CREATED);
        j.setTool(tool);
        j.setSeqrun(seqrun);
        j.setCreator(mgx.getCurrentUser());

        // fetch default parameters for the referenced tool
        Set<JobParameter> defaultParams = new HashSet<>();
        try {
            String toolXMLData = UnixHelper.readFile(new File(j.getTool().getXMLFile()));
            AutoCloseableIterator<JobParameter> jpIter = JobParameterHelper.getParameters(toolXMLData, mgxconfig.getPluginDump());
            while (jpIter.hasNext()) {
                defaultParams.add(jpIter.next());
            }
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        // postprocess user parameters
        for (JobParameter userParam : j.getParameters()) {
            JobParameter defaultParam = findDefaultParameter(userParam, defaultParams);
            String value = userParam.getParameterValue();

            if (defaultParam.getType().equals("ConfigFile")) {
                String fullPath;
                try {
                    fullPath = mgx.getProjectFileDirectory() + File.separator
                            + userParam.getParameterValue().substring(2).replace("|", File.separator);
                } catch (IOException ex) {
                    throw new MGXWebException(ex.getMessage());
                }
                if (!new File(fullPath).exists()) {
                    throw new MGXWebException("Invalid file path: " + userParam.getParameterValue());
                }
                value = fullPath;
            }
            userParam.setParameterValue(value);
        }

        // make sure all required parameters are set
        for (JobParameter defaultParam : defaultParams) {
            JobParameter userParam = findUserParameter(defaultParam, j.getParameters());
            if (userParam == null && !defaultParam.isOptional()) {
                throw new MGXWebException("Parameter " + defaultParam.getUserName() + " is required but missing.");
            }
        }

        // persist
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
            fixParameters(job);
            return JobDTOFactory.getInstance().toDTO(job);
        } catch (MGXException | IOException ex) {
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
        } catch (MGXException | IOException ex) {
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
                    String fullPath = mgx.getProjectFileDirectory() + File.separator
                            + jp.getParameterValue().substring(2).replace("|", File.separator);
                    jp.setParameterValue(fullPath);
                }
                job.getParameters().add(jp);
            }
            mgx.getJobDAO().update(job);
        } catch (MGXException | IOException ex) {
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
            verified = js.validate(mgx.getProjectName(), mgx.getDataSource(), job, dispConfig.getDispatcherHost(), mgx.getDatabaseHost(), mgx.getDatabaseName(), mgxconfig.getMGXUser(), mgxconfig.getMGXPassword(), mgx.getProjectDirectory());
        } catch (MGXException | MGXDispatcherException | IOException ex) {
            throw new MGXJobException(ex.getMessage());
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
            submitted = js.submit(dispConfig.getDispatcherHost(), mgx.getDataSource(), mgx.getProjectName(), job);
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
            RestartJob dJob = new RestartJob(mgx, dispConfig.getDispatcherHost(), mgxconfig, job, mgx.getDataSource(), mgx.getProjectName(), js);
            UUID taskId = taskHolder.addTask(dJob);
            return MGXString.newBuilder().setValue(taskId.toString()).build();
        } catch (MGXException | MGXDispatcherException | IOException ex) {
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
        Job job = null;
        try {
            job = mgx.getJobDAO().getById(id);
            JobState status = job.getStatus();
            // check if job has already reached a terminal state
            if (status == JobState.FAILED || status == JobState.FINISHED || status == JobState.ABORTED) {
                isActive = false;
            }
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        if (!isActive) {
            throw new MGXWebException("Job is not being processed, cannot cancel.");
        }

        mgx.log("Cancelling job " + id + " on user request");

        try {
            js.cancel(mgx.getProjectName(), id);
        } catch (MGXDispatcherException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ex.getMessage());
        }

        // update job state
        try {
            job.setStatus(JobState.ABORTED);
            mgx.getJobDAO().update(job);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
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
                js.delete(mgx.getProjectName(), id);
            } catch (MGXDispatcherException ex) {
                mgx.log(ex.getMessage());
            }
        }

        // remove persistent files
        try {
            mgx.getJobDAO().delete(id);
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        
        DeleteJob dJob = new DeleteJob(id, mgx.getDataSource(), mgx.getProjectName(), mappingSessions);
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
        } catch (MGXException | IOException ex) {
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

    private final Map<String, List<JobParameter>> paramCache = new HashMap<>();

    private void fixParameters(Job job) throws MGXException, IOException {
        String fName = job.getTool().getXMLFile();
        List<JobParameter> availableParams = null;

        if (paramCache.containsKey(fName)) {
            availableParams = paramCache.get(fName);
        } else {
            String toolXMLData = UnixHelper.readFile(new File(fName));
            availableParams = new ArrayList<>();
            AutoCloseableIterator<JobParameter> apIter = JobParameterHelper.getParameters(toolXMLData, mgxconfig.getPluginDump());
            while (apIter.hasNext()) {
                availableParams.add(apIter.next());
            }
            paramCache.put(fName, availableParams);
        }

        final String projectFileDir = mgx.getProjectFileDirectory().getAbsolutePath();

        for (JobParameter jp : job.getParameters()) {
            for (JobParameter candidate : availableParams) {
                // can't compare by ID field here
                if (jp.getNodeId() == candidate.getNodeId() && jp.getParameterName().equals(candidate.getParameterName())) {
                    jp.setClassName(candidate.getClassName());
                    jp.setType(candidate.getType());
                    jp.setDisplayName(candidate.getDisplayName());
                }
            }

            // do not expose internal path names
            if (jp.getParameterValue() != null && jp.getParameterValue().startsWith(projectFileDir)) {
                jp.setParameterValue(jp.getParameterValue().replaceAll(projectFileDir, ""));
            }
        }
    }

    private JobParameter findDefaultParameter(JobParameter userParam, Collection<JobParameter> defaultParams) {
        for (JobParameter jp : defaultParams) {
            if (jp.getNodeId() == userParam.getNodeId() && jp.getParameterName().equals(userParam.getParameterName())) {
                return jp;
            }
        }
        return null;
    }

    private JobParameter findUserParameter(JobParameter defaultParam, Collection<JobParameter> userParams) {
        for (JobParameter jp : userParams) {
            if (jp.getNodeId() == defaultParam.getNodeId() && jp.getParameterName().equals(defaultParam.getParameterName())) {
                return jp;
            }
        }
        return null;
    }
}
