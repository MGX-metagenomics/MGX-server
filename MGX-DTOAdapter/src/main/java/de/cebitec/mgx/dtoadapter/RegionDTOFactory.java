package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.RegionDTO;
import de.cebitec.mgx.dto.dto.RegionDTOList;
import de.cebitec.mgx.dto.dto.RegionDTOList.Builder;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class RegionDTOFactory extends DTOConversionBase<Region, RegionDTO, RegionDTOList> {

    static {
        instance = new RegionDTOFactory();
    }
    protected final static RegionDTOFactory instance;

    private RegionDTOFactory() {
    }

    public static RegionDTOFactory getInstance() {
        return instance;
    }

    @Override
    public RegionDTO toDTO(Region a) {
        RegionDTO.Builder dto = RegionDTO.newBuilder()
                .setId(a.getId())
                .setStart(a.getStart())
                .setStop(a.getStop())
                .setName(a.getName());
        if (a.getDescription() != null) {
            dto.setDescription(a.getDescription());
        }
        return dto.build();
    }

    @Override
    public Region toDB(RegionDTO dto) {
        Region r = new Region();
        if (dto.hasId()) {
            r.setId(dto.getId());
        }
        r.setName(dto.getName());
        r.setStart(dto.getStart());
        r.setStop(dto.getStop());
        if (dto.hasDescription()) {
            r.setDescription(dto.getDescription());
        }
        return r;
    }

    @Override
    public RegionDTOList toDTOList(AutoCloseableIterator<Region> acit) {
        Builder b = RegionDTOList.newBuilder();
        try (AutoCloseableIterator<Region> iter = acit) {
            while (iter.hasNext()) {
                b.addRegion(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        return b.build();
    }
}
