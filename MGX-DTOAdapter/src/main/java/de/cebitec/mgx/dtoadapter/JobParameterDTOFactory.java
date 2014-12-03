package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ChoicesDTO;
import de.cebitec.mgx.dto.dto.JobParameterDTO;
import de.cebitec.mgx.dto.dto.JobParameterDTO.Builder;
import de.cebitec.mgx.dto.dto.JobParameterListDTO;
import de.cebitec.mgx.dto.dto.KVPair;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class JobParameterDTOFactory extends DTOConversionBase<JobParameter, JobParameterDTO, JobParameterListDTO> {

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
                .setParameterName(p.getParameterName());

        if (p.getId() != null) {
            b = b.setId(p.getId());
        }

        //if (p.getUserName() != null) {
        b.setUserName(p.getUserName());
        //}
        //if (p.getUserDescription() != null) {
        b.setUserDesc(p.getUserDescription());
        //}
        if (p.getClassName() != null) {
            b.setClassName(p.getClassName());
        }
        if (p.getType() != null) {
            b.setType(p.getType());
        }
        if (p.getDisplayName() != null) {
            b.setDisplayName(p.getDisplayName());
        }
        if (p.isOptional() != null) {
            b.setIsOptional(p.isOptional());
        }
        // choices
        if (p.getChoices() != null) {
            ChoicesDTO.Builder choices = ChoicesDTO.newBuilder();
            for (Entry<String, String> e : p.getChoices().entrySet()) {
                KVPair kv = KVPair.newBuilder().setKey(e.getKey()).setValue(e.getValue()).build();
                choices.addEntry(kv);
            }
            b.setChoices(choices.build());
        }

        if (p.getParameterValue() != null) {
            b.setParameterValue(p.getParameterValue());
        }

        if (p.getDefaultValue() != null) {
            b.setDefaultValue(p.getDefaultValue());
        }
        return b.build();
    }

    @Override
    public JobParameter toDB(JobParameterDTO dto) {
        JobParameter jp = new JobParameter();
        if (dto.hasId() && dto.getId() != -1) {
            jp.setId(dto.getId());
        }
        jp.setNodeId(dto.getNodeId());
        jp.setUserName(dto.getUserName());
        jp.setUserDescription(dto.getUserDesc());
        jp.setDisplayName(dto.getDisplayName());
        jp.setClassName(dto.getClassName());
        jp.setType(dto.getType());
        jp.setOptional(dto.getIsOptional());

        jp.setParameterName(dto.getParameterName());
        jp.setParameterValue(dto.getParameterValue());

        if (dto.hasChoices()) {
            jp.setChoices(new HashMap<String, String>(dto.getChoices().getEntryCount()));
            for (KVPair kv : dto.getChoices().getEntryList()) {
                jp.getChoices().put(kv.getKey(), kv.getValue());
            }
        }

        if (dto.hasDefaultValue()) {
            jp.setDefaultValue(dto.getDefaultValue());
        }

        return jp;
    }

    @Override
    public JobParameterListDTO toDTOList(AutoCloseableIterator<JobParameter> acit) {
        JobParameterListDTO.Builder b = JobParameterListDTO.newBuilder();
        try (AutoCloseableIterator<JobParameter> iter = acit) {
            while (iter.hasNext()) {
                b.addParameter(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            log(ex.getMessage());
        }
        return b.build();
    }

    public Set<JobParameter> toDBList(JobParameterListDTO paramdtos) {
        Set<JobParameter> params = new HashSet<>();
        for (JobParameterDTO dto : paramdtos.getParameterList()) {
            params.add(toDB(dto));
        }
        return params;
    }
}
