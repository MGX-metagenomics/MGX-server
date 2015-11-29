package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGX;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.core.TaskI.State;
import de.cebitec.mgx.dto.dto.TaskDTO;
import de.cebitec.mgx.dtoadapter.TaskDTOFactory;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.util.UUID;
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
@Stateless
@Path("Task")
public class TaskBean {
    
    @Inject
    @MGX
    MGXController mgx;
    @EJB
    TaskHolder taskHolder;
    
    @GET
    @Path("get/{uuid}")
    @Produces("application/x-protobuf")
    public TaskDTO get(@PathParam("uuid") String uuid) {
        UUID taskId = UUID.fromString(uuid);
        TaskI task = taskHolder.getTask(taskId);
        if (task == null) {
            throw new MGXWebException("Invalid task ID: " + uuid);
        }
        if ((task.getState() == State.FAILED) || (task.getState() == State.FINISHED)) {
            taskHolder.removeTask(taskId);
        }
        return TaskDTOFactory.getInstance().toDTO(task);
    }
}
