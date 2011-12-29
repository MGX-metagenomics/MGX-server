package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.HabitatDTO;
import de.cebitec.mgx.dto.dto.HabitatDTOList;
import de.cebitec.mgx.dto.dto.HabitatDTOList.Builder;
import de.cebitec.mgx.model.db.Habitat;

/**
 *
 * @author sjaenick
 */
public class HabitatDTOFactory extends DTOConversionBase<Habitat, HabitatDTO, HabitatDTOList> {

    static {
        instance = new HabitatDTOFactory();
    }
    protected final static HabitatDTOFactory instance;

    private HabitatDTOFactory() {
    }

    public static HabitatDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final HabitatDTO toDTO(Habitat h) {
        return HabitatDTO.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setGpsLatitude(h.getLatitude())
                .setGpsLongitude(h.getLongitude())
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
                .setLatitude(dto.getGpsLatitude())
                .setLongitude(dto.getGpsLongitude())
                .setAltitude(dto.getAltitude())
                .setBiome(dto.getBiome())
                .setDescription(dto.getDescription());

        if (dto.hasId()) {
            h.setId(dto.getId());
        }

        return h;
    }

    @Override
    public HabitatDTOList toDTOList(Iterable<Habitat> list) {
        Builder b = HabitatDTOList.newBuilder();
        for (Habitat o : list) {
            b.addHabitat(toDTO(o));
        }
        return b.build();
    }

}
