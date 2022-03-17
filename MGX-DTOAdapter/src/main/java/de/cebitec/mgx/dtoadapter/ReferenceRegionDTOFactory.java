package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTO;
import de.cebitec.mgx.dto.dto.ReferenceRegionDTOList;
import de.cebitec.mgx.model.db.ReferenceRegion;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class ReferenceRegionDTOFactory extends DTOConversionBase<ReferenceRegion, ReferenceRegionDTO, ReferenceRegionDTOList> {

    static {
        instance = new ReferenceRegionDTOFactory();
    }
    protected final static ReferenceRegionDTOFactory instance;

    private ReferenceRegionDTOFactory() {
    }

    public static ReferenceRegionDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ReferenceRegionDTO toDTO(ReferenceRegion a) {
        ReferenceRegionDTO.Builder b = ReferenceRegionDTO.newBuilder()
                .setId(a.getId())
                .setStart(a.getStart())
                .setStop(a.getStop())
                .setName(a.getName())
                .setType(dto.RegionType.forNumber(a.getType().getValue()))
                .setParentId(a.getReferenceId());
        if (a.getDescription() != null) {
            b.setDescription(a.getDescription());
        }
        return b.build();
    }

    @Override
    public ReferenceRegion toDB(ReferenceRegionDTO dto) {
        ReferenceRegion r = new ReferenceRegion();
        if (dto.getId() > 0) {
            r.setId(dto.getId());
        }
        r.setReferenceId(dto.getParentId());
        r.setName(dto.getName());
        r.setType(RegionType.values()[dto.getType().ordinal()]);
        r.setStart(dto.getStart());
        r.setStop(dto.getStop());
        if (!dto.getDescription().isEmpty()) {
            r.setDescription(dto.getDescription());
        }
        return r;
    }

    @Override
    public ReferenceRegionDTOList toDTOList(AutoCloseableIterator<ReferenceRegion> acit) {
        ReferenceRegionDTOList.Builder b = ReferenceRegionDTOList.newBuilder();
        try (AutoCloseableIterator<ReferenceRegion> iter = acit) {
            while (iter.hasNext()) {
                b.addRegion(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        return b.build();
    }
}
