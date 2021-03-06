package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ObservationDTO;
import de.cebitec.mgx.dto.dto.ObservationDTOList;
import de.cebitec.mgx.dto.dto.ObservationDTOList.Builder;
import de.cebitec.mgx.model.misc.SequenceObservation;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class ObservationDTOFactory extends DTOConversionBase<SequenceObservation, ObservationDTO, ObservationDTOList> {

    static {
        instance = new ObservationDTOFactory();
    }
    protected final static ObservationDTOFactory instance;

    private ObservationDTOFactory() {
    }

    public static ObservationDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ObservationDTO toDTO(SequenceObservation a) {
        return ObservationDTO.newBuilder()
                .setStart(a.getStart())
                .setStop(a.getStop())
                .setAttributeName(a.getAttributeName())
                .setAttributeTypeValue(a.getAttributeTypeName())
                .build();
    }

    @Override
    public SequenceObservation toDB(ObservationDTO dto) {
        return new SequenceObservation(dto.getStart(), dto.getStop(), dto.getAttributeName(), dto.getAttributeTypeValue());
    }
    
    public List<SequenceObservation> toDBList(ObservationDTOList dtoList) {
        List<SequenceObservation> obs = new ArrayList<>(dtoList.getObservationCount());
        for (ObservationDTO dto : dtoList.getObservationList()) {
            obs.add(toDB(dto));
        }
        return obs;
    }

    @Override
    public ObservationDTOList toDTOList(AutoCloseableIterator<SequenceObservation> acit) {
        Builder b = ObservationDTOList.newBuilder();
        try (AutoCloseableIterator<SequenceObservation> iter = acit) {
            while (iter.hasNext()) {
                b.addObservation(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
