package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.common.JobState;
import de.cebitec.mgx.dto.dto.JobDTO;
import de.cebitec.mgx.dto.dto.JobDTO.Builder;
import de.cebitec.mgx.dto.dto.JobDTOList;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class JobDTOFactory extends DTOConversionBase<Job, JobDTO, JobDTOList> {

    static {
        instance = new JobDTOFactory();
    }
    protected final static JobDTOFactory instance;

    private JobDTOFactory() {
    }

    public static JobDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final JobDTO toDTO(Job j) {
        Builder b = JobDTO.newBuilder()
                .setId(j.getId())
                .setSeqrunId(j.getSeqrunId())
                .setToolId(j.getToolId())
                .setCreator(j.getCreator())
                .setState(JobDTO.JobState.forNumber(j.getStatus().getValue()));

        AutoCloseableIterator<JobParameter> acit = new ForwardingIterator<>(j.getParameters().iterator());
        b.setParameters(JobParameterDTOFactory.getInstance().toDTOList(acit));

        if (j.getStartDate() != null) {
            b.setStartDate(toUnixTimeStamp(j.getStartDate()));
        }

        if (j.getFinishDate() != null) {
            b.setFinishDate(toUnixTimeStamp(j.getFinishDate()));
        }

        return b.build();
    }

    @Override
    public final Job toDB(JobDTO dto) {
        Job j = new Job()
                .setStatus(JobState.values()[dto.getState().ordinal()])
                .setCreator(dto.getCreator())
                .setStartDate(toDate(dto.getStartDate()))
                .setFinishDate(toDate(dto.getFinishDate()))
                .setParameters(JobParameterDTOFactory.getInstance().toDBList(dto.getParameters()));

        if (dto.hasId() && dto.getId() != -1) {
            j.setId(dto.getId());
        }
        for (JobParameter jp : j.getParameters()) {
            jp.setJobId(j.getId());
        }

        return j;
    }

    @Override
    public JobDTOList toDTOList(AutoCloseableIterator<Job> acit) {
        JobDTOList.Builder b = JobDTOList.newBuilder();
        try (AutoCloseableIterator<Job> iter = acit) {
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
