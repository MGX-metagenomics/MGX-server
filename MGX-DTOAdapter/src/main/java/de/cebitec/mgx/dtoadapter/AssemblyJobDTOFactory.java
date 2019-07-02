package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.AssemblyJobDTO;
import de.cebitec.mgx.dto.dto.AssemblyJobDTO.Builder;
import de.cebitec.mgx.dto.dto.AssemblyJobDTOList;
import de.cebitec.mgx.model.db.AssemblyJob;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class AssemblyJobDTOFactory extends DTOConversionBase<AssemblyJob, AssemblyJobDTO, AssemblyJobDTOList> {

    static {
        instance = new AssemblyJobDTOFactory();
    }
    protected final static AssemblyJobDTOFactory instance;

    private AssemblyJobDTOFactory() {
    }

    public static AssemblyJobDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final AssemblyJobDTO toDTO(AssemblyJob j) {
        Builder b = AssemblyJobDTO.newBuilder()
                .setId(j.getId());
        
        for (Long l : j.getSeqrunIds()) {
            b.addSeqrun(l);
        }
        
        b.setCreatedBy(j.getCreator());
        b.setState(dto.JobState.forNumber(j.getStatus().getValue()));

        if (j.getStartDate() != null) {
            b.setStartDate(toUnixTimeStamp(j.getStartDate()));
        }

        if (j.getFinishDate() != null) {
            b.setFinishDate(toUnixTimeStamp(j.getFinishDate()));
        }

        return b.build();
    }

    @Override
    public final AssemblyJob toDB(AssemblyJobDTO dto) {
        AssemblyJob j = new AssemblyJob()
                .setStatus(JobState.values()[dto.getState().ordinal()])
                .setCreator(dto.getCreatedBy())
                .setStartDate(toDate(dto.getStartDate()))
                .setFinishDate(toDate(dto.getFinishDate()));
        
        j.setSeqruns(dto.getSeqrunList().toArray(new Long[]{}));

        if (dto.hasId() && dto.getId() != -1) {
            j.setId(dto.getId());
        }

        return j;
    }

    @Override
    public AssemblyJobDTOList toDTOList(AutoCloseableIterator<AssemblyJob> acit) {
        AssemblyJobDTOList.Builder b = AssemblyJobDTOList.newBuilder();
        try (AutoCloseableIterator<AssemblyJob> iter = acit) {
            while (iter.hasNext()) {
                b.addJob(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return b.build();
    }
}
