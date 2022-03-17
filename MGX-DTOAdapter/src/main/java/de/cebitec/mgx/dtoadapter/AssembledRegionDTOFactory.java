package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.common.RegionType;
import de.cebitec.mgx.dto.dto;
import de.cebitec.mgx.dto.dto.AssembledRegionDTO;
import de.cebitec.mgx.dto.dto.AssembledRegionDTOList;
import de.cebitec.mgx.model.db.AssembledRegion;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class AssembledRegionDTOFactory extends DTOConversionBase<AssembledRegion, AssembledRegionDTO, AssembledRegionDTOList> {

    static {
        instance = new AssembledRegionDTOFactory();
    }
    protected final static AssembledRegionDTOFactory instance;

    private AssembledRegionDTOFactory() {
    }

    public static AssembledRegionDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final AssembledRegionDTO toDTO(AssembledRegion h) {
        return AssembledRegionDTO.newBuilder()
                .setId(h.getId())
                .setStart(h.getStart())
                .setStop(h.getStop())
                .setType(dto.RegionType.forNumber(h.getType().getValue()))
                .setCoverage(h.getCoverage())
                .setContigId(h.getContigId())
                .build();
    }

    @Override
    public final AssembledRegion toDB(AssembledRegionDTO dto) {
        AssembledRegion h = new AssembledRegion();
        h.setStart(dto.getStart());
        h.setStop(dto.getStop());
        h.setCoverage(dto.getCoverage());
        h.setContigId(dto.getContigId());
        h.setType(RegionType.values()[dto.getType().ordinal()]);

        if (dto.getId() != 0) {
            h.setId(dto.getId());
        }

        return h;
    }

    @Override
    public AssembledRegionDTOList toDTOList(AutoCloseableIterator<AssembledRegion> acit) {
        AssembledRegionDTOList.Builder b = AssembledRegionDTOList.newBuilder();
        try (AutoCloseableIterator<AssembledRegion> iter = acit) {
            while (iter.hasNext()) {
                b.addRegion(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
