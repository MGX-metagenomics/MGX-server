package de.cebitec.mgx.web;

import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.core.TaskI.State;
import de.cebitec.mgx.dto.dto.TaskDTO;
import de.cebitec.mgx.dtoadapter.TaskDTOFactory;
import de.cebitec.mgx.sessions.TaskHolder;
import de.cebitec.mgx.web.exception.MGXWebException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.UUID;

/**
 *
 * @author sjaenick
 */
@Stateless
@Path("Task")
public class TaskBean {
    
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
