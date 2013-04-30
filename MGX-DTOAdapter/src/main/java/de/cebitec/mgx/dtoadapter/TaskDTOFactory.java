package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.TaskDTO;
import de.cebitec.mgx.dto.dto.TaskDTOList;
import de.cebitec.mgx.sessions.TaskI;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class TaskDTOFactory extends DTOConversionBase<TaskI, TaskDTO, TaskDTOList> {

    static {
        instance = new TaskDTOFactory();
    }
    protected final static TaskDTOFactory instance;

    private TaskDTOFactory() {
    }

    public static TaskDTOFactory getInstance() {
        return instance;
    }

    @Override
    public TaskDTO toDTO(TaskI t) {
        return TaskDTO.newBuilder()
                .setMessage(t.getStatusMessage())
                .setState(TaskDTO.TaskState.valueOf(t.getState().getValue()))
                .build();
    }

    @Override
    public TaskI toDB(TaskDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TaskDTOList toDTOList(AutoCloseableIterator<TaskI> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
