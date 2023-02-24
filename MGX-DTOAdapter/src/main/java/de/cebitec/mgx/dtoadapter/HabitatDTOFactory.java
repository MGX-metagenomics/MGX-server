package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.HabitatDTO;
import de.cebitec.mgx.dto.dto.HabitatDTOList;
import de.cebitec.mgx.dto.dto.HabitatDTOList.Builder;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.util.AutoCloseableIterator;

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
                .setBiome(dto.getBiome())
                .setDescription(dto.getDescription());

        if (dto.getId() != 0) {
            h.setId(dto.getId());
        }

        return h;
    }

    @Override
    public HabitatDTOList toDTOList(AutoCloseableIterator<Habitat> acit) {
        Builder b = HabitatDTOList.newBuilder();
        try (AutoCloseableIterator<Habitat> iter = acit) {
            while (iter.hasNext()) {
                b.addHabitat(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
