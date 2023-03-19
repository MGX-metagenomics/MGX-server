package de.cebitec.mgx.web;

import de.cebitec.gpms.security.Secure;
import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.commonwl.CommonWL;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXRoles;
import de.cebitec.mgx.conveyor.JobParameterHelper;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.dispatcher.common.api.MGXDispatcherException;
import de.cebitec.mgx.dispatcher.common.api.MGXInsufficientJobConfigurationException;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.MGXBoolean;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dtoadapter.JobDTOFactory;
import de.cebitec.mgx.dtoadapter.JobParameterDTOFactory;
import de.cebitec.mgx.jobsubmitter.api.Host;
import de.cebitec.mgx.jobsubmitter.api.JobSubmitterI;
import de.cebitec.mgx.model.db.*;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.web.exception.MGXJobException;
import de.cebitec.mgx.web.exception.MGXWebException;
import de.cebitec.mgx.web.helper.ExceptionMessageConverter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import de.cebitec.mgx.dispatcher.common.api.DispatcherClientConfigurationI;
import de.cebitec.mgx.global.MGXGlobal;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    MGXGlobal global;
    @EJB
    JobSubmitterI js;
    @EJB
    TaskHolder taskHolder;
    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    DispatcherClientConfigurationI dispConfig;

    @PUT
    @Path("runDefaultTools")
    @Consumes("application/x-protobuf")
    public Response runDefaultTools(MGXString dto) {
        long seqrunId = Long.parseLong(dto.getValue());
        try {
            // fetch run to make sure it exists
            SeqRun run = mgx.getSeqRunDAO().getById(seqrunId);

            Result<AutoCloseableIterator<Tool>> globalT = global.getToolDAO().getAll();
            if (globalT.isError()) {
                throw new MGXWebException(globalT.getError());
            }
            Iterator<Tool> tIter = globalT.getValue();
            List<Tool> globalTools = new ArrayList<>();
            while (tIter != null && tIter.hasNext()) {
                globalTools.add(tIter.next());
            }

            List<Tool> defaultTools = mgx.getToolDAO().getDefaultTools(globalTools);

            for (Tool t : defaultTools) {
                Job j = new Job();
                j.setStatus(JobState.VERIFIED);
                j.setToolId(t.getId());
                j.setSeqrunIds(new long[]{seqrunId});
                j.setCreator(mgx.getCurrentUser());

                // fetch default parameters for the tool
                Set<JobParameter> defaultParams = new HashSet<>();
                String toolXMLData = UnixHelper.readFile(new File(t.getFile()));
                AutoCloseableIterator<JobParameter> jpIter = JobParameterHelper.getParameters(toolXMLData, mgxconfig.getPluginDump());
                while (jpIter.hasNext()) {
                    JobParameter jp = jpIter.next();
                    jp.setParameterValue(jp.getDefaultValue());
                    defaultParams.add(jp);
                }
                j.setParameters(defaultParams);

                createJob(j);

                if (t.getFile().endsWith("xml")) {
                    mgx.getJobDAO().writeConveyorConfigFile(j, mgxconfig.getAnnotationService(), mgx.getProjectName(), mgx.getProjectDirectory(),
                            mgxconfig.getMGXUser(), mgxconfig.getMGXPassword(), mgx.getDatabaseName(), mgx.getDatabaseHost(), mgx.getDatabasePort());
                } else if (t.getFile().endsWith("cwl")) {
                    mgx.getJobDAO().writeCWLConfigFile(j, mgx.getProjectDirectory(), mgx.getProjectName(), mgxconfig.getAnnotationService());
                } else {
                    throw new MGXWebException("Unable to determine workflow type.");
                }
                js.submit(new Host(dispConfig.getDispatcherHost()), mgx.getProjectName(), j.getId());
            }

        } catch (MGXException | MGXDispatcherException | IOException ex) {
            mgx.log(ex);
            throw new MGXWebException(ex.getMessage());
        }

        return Response.ok().build();
    }

    @PUT
    @Path("create")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXLong create(JobDTO dto) {

        Job j = JobDTOFactory.getInstance().toDB(dto);

        j.setStatus(JobState.CREATED);
        j.setToolId(dto.getToolId());

        j.setCreator(mgx.getCurrentUser());

        long jobId;
        try {
            jobId = createJob(j);
        } catch (MGXException ex) {
            mgx.log(ex);
            throw new MGXWebException(ex.getMessage());
        }

        return MGXLong.newBuilder().setValue(jobId).build();
    }

    public long createJob(Job j) throws MGXException {

        if ((j.getSeqrunIds() == null || j.getSeqrunIds().length == 0) && j.getAssemblyId() == Identifiable.INVALID_IDENTIFIER) {
            throw new MGXException("Job object does not reference sequencing runs or assemblies.");
        }

        // fetch default parameters for the referenced tool
        Set<JobParameter> defaultParams = new HashSet<>();
        Tool tool = null;
        try {
            tool = mgx.getToolDAO().getById(j.getToolId());
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        try {
            if (tool.getFile().endsWith(".xml")) {
                String toolXMLData = UnixHelper.readFile(new File(tool.getFile()));
                AutoCloseableIterator<JobParameter> jpIter = JobParameterHelper.getParameters(toolXMLData, mgxconfig.getPluginDump());
                while (jpIter.hasNext()) {
                    defaultParams.add(jpIter.next());
                }
            } else { // CWL
                AutoCloseableIterator<JobParameter> parameters = CommonWL.getParameters(UnixHelper.readFile(new File(tool.getFile())));
                while (parameters.hasNext()) {
                    defaultParams.add(parameters.next());
                }
            }
        } catch (MGXException | IOException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        // postprocess user parameters
        if (tool.getFile().endsWith(".xml")) {
            for (JobParameter userParam : j.getParameters()) {
                JobParameter defaultParam = findDefaultParameter(userParam, defaultParams);
                String value = userParam.getParameterValue();

                if (defaultParam.getType() == null) {
                    mgx.log("Null type for " + defaultParam);
                    throw new MGXWebException("No type for parameter");
                }

                if ("ConfigFile".equals(defaultParam.getType())) {
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
        }

        // make sure all required parameters are set
        for (JobParameter defaultParam : defaultParams) {
            JobParameter userParam = findUserParameter(defaultParam, j.getParameters());
            if (userParam == null && !defaultParam.isOptional()) {
                throw new MGXWebException("Parameter " + defaultParam.getUserName() + " is required but missing.");
            }
        }

        // persist
        long job_id;
        try {
            job_id = mgx.getJobDAO().create(j);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        return job_id;
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
        try {
            AutoCloseableIterator<Job> acit = mgx.getJobDAO().getAll();
            return JobDTOFactory.getInstance().toDTOList(acit);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("getParameters/{id}")
    @Produces("application/x-protobuf")
    public JobParameterListDTO getParameters(@PathParam("id") Long job_id) {
        try {
            AutoCloseableIterator<JobParameter> params = mgx.getJobParameterDAO().byJob(job_id);
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
                jp.setJobId(id);

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
            Job job = mgx.getJobDAO().getById(id, false);

            if ((job.getSeqrunIds() == null || job.getSeqrunIds().length == 0) && job.getAssemblyId() == Identifiable.INVALID_IDENTIFIER) {
                throw new MGXException("Job object does not reference sequencing runs or assemblies.");
            }

            Tool t = mgx.getToolDAO().getById(job.getToolId());

            if (t.getFile().endsWith("xml")) {
                mgx.getJobDAO().writeConveyorConfigFile(job, mgxconfig.getAnnotationService(),
                        mgx.getProjectName(),
                        mgx.getProjectDirectory(), mgxconfig.getMGXUser(), mgxconfig.getMGXPassword(),
                        mgx.getDatabaseName(), mgx.getDatabaseHost(), mgx.getDatabasePort());
            } else if (t.getFile().endsWith("cwl")) {
                // as the stored job parameters do not have a class name, we
                // need to fetch these from the workflow and update the params
                String toolContent = UnixHelper.readFile(new File(t.getFile()));
                AutoCloseableIterator<JobParameter> params = CommonWL.getParameters(toolContent);
                Map<String, JobParameter> tmp = new HashMap<>();
                while (params.hasNext()) {
                    JobParameter jp = params.next();
                    tmp.put(jp.getUserName(), jp);
                }
                for (JobParameter jp : job.getParameters()) {
                    jp.setClassName(tmp.get(jp.getUserName()).getClassName());
                }
                mgx.getJobDAO().writeCWLConfigFile(job, mgx.getProjectDirectory(), mgx.getProjectName(), mgxconfig.getAnnotationService());
            } else {
                throw new MGXWebException("Unable to determine workflow type.");
            }

            verified = js.validate(new Host(dispConfig.getDispatcherHost()), mgx.getProjectName(), job.getId());
        } catch (MGXException | MGXDispatcherException | IOException ex) {
            mgx.log(ex);
            throw new MGXJobException(ex.getMessage());
        }

        return MGXBoolean.newBuilder().setValue(verified).build();
    }

    @GET
    @Path("execute/{id}")
    @Produces("application/x-protobuf")
    @Secure(rightsNeeded = {MGXRoles.User, MGXRoles.Admin})
    public MGXBoolean execute(@PathParam("id") Long jobId) {

        boolean submitted = false;
        try {
            Job job = mgx.getJobDAO().getById(jobId);

            if ((job.getSeqrunIds() == null || job.getSeqrunIds().length == 0) && job.getAssemblyId() == Identifiable.INVALID_IDENTIFIER) {
                throw new MGXException("Job object does not reference sequencing runs or assemblies.");
            }

            if (job.getStatus() != JobState.VERIFIED) {
                throw new MGXWebException("Job %d in invalid state %s", jobId, job.getStatus());
            }
            submitted = js.submit(new Host(dispConfig.getDispatcherHost()), mgx.getProjectName(), jobId);

            job.setStatus(submitted ? JobState.SUBMITTED : JobState.FAILED);
            mgx.getJobDAO().update(job);

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
        try {
            Job job = mgx.getJobDAO().getById(id);
            JobState status = job.getStatus();
            if (status != JobState.FAILED && status != JobState.ABORTED) {
                throw new MGXWebException("Job is in invalid state.");
            }

            //
            // re-create the jobs config file to account for changes, e.g.
            // when the database was moved to a different server
            //
            Tool t = mgx.getToolDAO().getById(job.getToolId());
            if (t.getFile().endsWith("xml")) {
                mgx.getJobDAO().writeConveyorConfigFile(job, mgxconfig.getAnnotationService(),
                        mgx.getProjectName(),
                        mgx.getProjectDirectory(), mgxconfig.getMGXUser(), mgxconfig.getMGXPassword(),
                        mgx.getDatabaseName(), mgx.getDatabaseHost(), mgx.getDatabasePort());
            } else if (t.getFile().endsWith("cwl")) {
                // as the stored job parameters do not have a class name, we
                // need to fetch these from the workflow and update the params
                String toolContent = UnixHelper.readFile(new File(t.getFile()));
                AutoCloseableIterator<JobParameter> params = CommonWL.getParameters(toolContent);
                Map<String, JobParameter> tmp = new HashMap<>();
                while (params.hasNext()) {
                    JobParameter jp = params.next();
                    tmp.put(jp.getUserName(), jp);
                }
                for (JobParameter jp : job.getParameters()) {
                    jp.setClassName(tmp.get(jp.getUserName()).getClassName());
                }
                mgx.getJobDAO().writeCWLConfigFile(job, mgx.getProjectDirectory(), mgx.getProjectName(), mgxconfig.getAnnotationService());
            } else {
                throw new MGXWebException("Unable to determine workflow type.");
            }

            TaskI task = mgx.getJobDAO().restart(job, dispConfig.getDispatcherHost(), mgx.getDataSource(), mgx.getProjectName(), js);
            UUID taskId = taskHolder.addTask(task);
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
        Job job = null;
        try {
            job = mgx.getJobDAO().getById(id);
            JobState status = job.getStatus();
            // check if job has already reached a terminal state
            if (status == JobState.FAILED || status == JobState.FINISHED || status == JobState.ABORTED) {
                throw new MGXWebException("Job is not being processed, cannot cancel.");
            }
        } catch (MGXException ex) {
            mgx.log(ex.getMessage());
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }

        mgx.log("Cancelling job " + id + " on user request");

        try {
            Host h = new Host(dispConfig.getDispatcherHost());
            js.cancel(h, mgx.getProjectName(), id);
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
        Job job = JobDTOFactory.getInstance().toDB(dto);
        try {
            job.setToolId(dto.getToolId());
            long[] temp = new long[dto.getSeqrunCount()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = dto.getSeqrun(i);
            }
            job.setSeqrunIds(temp);
            mgx.getJobDAO().update(job);
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
                Host h = new Host(dispConfig.getDispatcherHost());
                js.delete(h, mgx.getProjectName(), id);
            } catch (MGXDispatcherException ex) {
                mgx.log(ex.getMessage());
            }
        }

        UUID taskId;
        try {
            taskId = taskHolder.addTask(mgx.getJobDAO().delete(id));
        } catch (IOException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
        return MGXString.newBuilder().setValue(taskId.toString()).build();
    }

    @GET
    @Path("BySeqRun/{seqrun_id}")
    @Produces("application/x-protobuf")
    public JobDTOList BySeqRun(@PathParam("seqrun_id") Long seqrun_id) {
        try {
            SeqRun run = mgx.getSeqRunDAO().getById(seqrun_id);
            AutoCloseableIterator<Job> acit = mgx.getJobDAO().bySeqRun(seqrun_id);
            return JobDTOFactory.getInstance().toDTOList(acit);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
    }

    @GET
    @Path("ByAssembly/{asm_id}")
    @Produces("application/x-protobuf")
    public JobDTOList ByAssembly(@PathParam("asm_id") Long asm_id) {
        try {
            Assembly asm = mgx.getAssemblyDAO().getById(asm_id);
            AutoCloseableIterator<Job> acit = mgx.getJobDAO().byAssembly(asm_id);
            return JobDTOFactory.getInstance().toDTOList(acit);
        } catch (MGXException ex) {
            throw new MGXWebException(ExceptionMessageConverter.convert(ex.getMessage()));
        }
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

//    private final Map<String, List<JobParameter>> paramCache = new HashMap<>();
//
//    private void fixParameters(Job job) throws MGXException, IOException {
//        String fName = job.getTool().getXMLFile();
//        List<JobParameter> availableParams;
//
//        if (paramCache.containsKey(fName)) {
//            availableParams = paramCache.get(fName);
//        } else {
//            String toolXMLData = UnixHelper.readFile(new File(fName));
//            availableParams = new ArrayList<>();
//            AutoCloseableIterator<JobParameter> apIter = JobParameterHelper.getParameters(toolXMLData, mgxconfig.getPluginDump());
//            while (apIter.hasNext()) {
//                availableParams.add(apIter.next());
//            }
//            paramCache.put(fName, availableParams);
//        }
//
//        final String projectFileDir = mgx.getProjectFileDirectory().getAbsolutePath();
//
//        for (JobParameter jp : job.getParameters()) {
//            for (JobParameter candidate : availableParams) {
//                // can't compare by ID field here
//                if (jp.getNodeId() == candidate.getNodeId() && jp.getParameterName().equals(candidate.getParameterName())) {
//                    jp.setClassName(candidate.getClassName());
//                    jp.setType(candidate.getType());
//                    jp.setDisplayName(candidate.getDisplayName());
//                }
//            }
//
//            // do not expose internal path names
//            if (jp.getParameterValue() != null && jp.getParameterValue().startsWith(projectFileDir + File.separator)) {
//                jp.setParameterValue(jp.getParameterValue().replaceAll(projectFileDir + File.separator, ""));
//            }
//            if (jp.getParameterValue() != null && jp.getParameterValue().startsWith(projectFileDir)) {
//                jp.setParameterValue(jp.getParameterValue().replaceAll(projectFileDir, ""));
//            }
//        }
//    }
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
