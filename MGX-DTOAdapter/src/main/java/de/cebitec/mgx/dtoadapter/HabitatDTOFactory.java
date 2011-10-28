package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.HabitatDTO;
import de.cebitec.mgx.model.db.Habitat;

/**
 *
 * @author sjaenick
 */
public class HabitatDTOFactory extends DTOConversionBase<Habitat, HabitatDTO> {

    static {
        instance = new HabitatDTOFactory();
    }
    protected static HabitatDTOFactory instance;

    private HabitatDTOFactory() {}

    public static HabitatDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final HabitatDTO toDTO(Habitat h) {
        return HabitatDTO.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setGPSlocation(h.getLocation())
                .setAltitude(h.getAltitude())
                .setBiome(h.getBiome())
                .setDescription(h.getDescription())
                .setId(h.getId())
                .build();
    }

    @Override
    public final Habitat toDB(HabitatDTO dto) {
        Habitat h = new Habitat()
                .setName(dto.getName())
                .setLocation(dto.getGPSlocation())
                .setAltitude(dto.getAltitude())
                .setBiome(dto.getBiome())
                .setDescription(dto.getDescription());

        if (dto.hasId())
            h.setId(dto.getId());

        return h;
    }
}
