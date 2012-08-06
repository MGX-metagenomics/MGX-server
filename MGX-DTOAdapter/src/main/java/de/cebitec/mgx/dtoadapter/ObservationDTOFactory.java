package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ObservationDTO;
import de.cebitec.mgx.dto.dto.ObservationDTOList;
import de.cebitec.mgx.dto.dto.ObservationDTOList.Builder;
import de.cebitec.mgx.util.SequenceObservation;

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
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ObservationDTOList toDTOList(Iterable<SequenceObservation> list) {
        Builder b = ObservationDTOList.newBuilder();
        for (SequenceObservation obs : list) {
            b.addObservation(toDTO(obs));
        }
        return b.build();
    }
}