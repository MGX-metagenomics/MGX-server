package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTO.Builder;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobState;

/**
 *
 * @author sjaenick
 */
public class JobDTOFactory extends DTOConversionBase<Job, JobDTO, JobDTOList> {

    static {
        instance = new JobDTOFactory();
    }
    protected final static JobDTOFactory instance;

    private JobDTOFactory() {}

    public static JobDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final JobDTO toDTO(Job j) {
        Builder b = JobDTO.newBuilder()
                .setId(j.getId())
                .setSeqrunId(j.getSeqrun().getId())
                .setToolId(j.getTool().getId())
                .setCreator(j.getCreator())
                .setState(JobDTO.JobState.valueOf(j.getStatus().getValue()));

        if (j.getStartDate() != null)
            b.setStartDate(toUnixTimeStamp(j.getStartDate()));

        if (j.getFinishDate() != null)
            b.setFinishDate(toUnixTimeStamp(j.getFinishDate()));

        return b.build();
    }

    @Override
    public final Job toDB(JobDTO dto) {
        Job j = new Job()
                .setStatus(JobState.values()[dto.getState().ordinal()])
                .setCreator(dto.getCreator())
                .setStartDate(toDate(dto.getStartDate()))
                .setFinishDate(toDate(dto.getFinishDate()));

        if (dto.hasId())
            j.setId(dto.getId());

        return j;
    }

    @Override
    public JobDTOList toDTOList(Iterable<Job> list) {
        JobDTOList.Builder b = JobDTOList.newBuilder();
        for (Job job : list) {
            b.addJob(toDTO(job));
        }
        return b.build();
    }
}
