package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.JobParameterDTO;
import de.cebitec.mgx.dto.dto.JobParameterDTO.Builder;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.util.JobParameter;

/**
 *
 * @author sjaenick
 */
public class JobParameterDTOFactory extends DTOConversionBase<JobParameter, JobParameterDTO, dto.JobParameterListDTO> {

    static {
        instance = new JobParameterDTOFactory();
    }
    protected final static JobParameterDTOFactory instance;

    private JobParameterDTOFactory() {
    }

    public static JobParameterDTOFactory getInstance() {
        return instance;
    }

    @Override
    public JobParameterDTO toDTO(JobParameter p) {
        Builder b = JobParameterDTO.newBuilder()
                .setNodeId(p.getNodeId())
                .setUserName(p.getUserName())
                .setUserDesc(p.getUserDescription())
                .setConfigitemName(p.getConfigItemName())
                .setType(p.getType())
                .setIsOptional(p.isOptional());
        
        if (!"".equals(p.getDefaultValue())) {
            b.setDefaultValue(p.getDefaultValue());
        }
        if (p.getConfigItemValue() != null) {
            b.setConfigitemValue(p.getConfigItemValue());
        }
        return b.build();
    }

    @Override
    public JobParameter toDB(JobParameterDTO dto) {
        JobParameter jp = new JobParameter();
        jp.setNodeId(dto.getNodeId());
        jp.setUserName(dto.getUserName());
        jp.setUserDescription(dto.getUserDesc());
        jp.setConfigItemName(dto.getConfigitemName());
        jp.setType(dto.getType());
        jp.setOptional(dto.getIsOptional());
        if (dto.hasDefaultValue()) {
            jp.setDefaultValue(dto.getDefaultValue());
        }
        if (dto.hasConfigitemValue()) {
            jp.setConfigItemValue(dto.getConfigitemValue());
        }
        
        return jp;
    }

    @Override
    public JobParameterListDTO toDTOList(Iterable<JobParameter> list) {
        JobParameterListDTO.Builder b = JobParameterListDTO.newBuilder();
        for (JobParameter jp : list) {
            b.addParameter(toDTO(jp));
        }
        return b.build();
    }
}
