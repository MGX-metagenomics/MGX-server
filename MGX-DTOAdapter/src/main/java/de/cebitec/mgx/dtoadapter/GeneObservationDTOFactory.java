package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.GeneObservationDTO;
import de.cebitec.mgx.dto.dto.GeneObservationDTOList;
import de.cebitec.mgx.model.misc.GeneObservation;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class GeneObservationDTOFactory extends DTOConversionBase<GeneObservation, GeneObservationDTO, GeneObservationDTOList> {

    static {
        instance = new GeneObservationDTOFactory();
    }
    protected final static GeneObservationDTOFactory instance;

    private GeneObservationDTOFactory() {
    }

    public static GeneObservationDTOFactory getInstance() {
        return instance;
    }

    @Override
    public GeneObservationDTO toDTO(GeneObservation a) {
        return GeneObservationDTO.newBuilder()
                .setStart(a.getStart())
                .setStop(a.getStop())
                .setAttributeName(a.getAttributeName())
                .setAttributeTypeValue(a.getAttributeTypeName())
                .build();
    }

    @Override
    public GeneObservation toDB(GeneObservationDTO dto) {
        return new GeneObservation(dto.getStart(), dto.getStop(), dto.getAttributeName(), dto.getAttributeTypeValue());
    }
    
    public List<GeneObservation> toDBList(GeneObservationDTOList dtoList) {
        List<GeneObservation> obs = new ArrayList<>(dtoList.getObservationCount());
        for (GeneObservationDTO dto : dtoList.getObservationList()) {
            obs.add(toDB(dto));
        }
        return obs;
    }

    @Override
    public GeneObservationDTOList toDTOList(AutoCloseableIterator<GeneObservation> acit) {
        GeneObservationDTOList.Builder b = GeneObservationDTOList.newBuilder();
        try (AutoCloseableIterator<GeneObservation> iter = acit) {
            while (iter.hasNext()) {
                b.addObservation(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
