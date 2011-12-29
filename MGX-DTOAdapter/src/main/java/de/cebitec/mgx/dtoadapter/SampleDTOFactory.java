package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.SampleDTO;
import de.cebitec.mgx.dto.dto.SampleDTOList;
import de.cebitec.mgx.dto.dto.SampleDTOList.Builder;
import de.cebitec.mgx.model.db.Sample;

/**
 *
 * @author sjaenick
 */
public class SampleDTOFactory extends DTOConversionBase<Sample, SampleDTO, SampleDTOList> {

    static {
        instance = new SampleDTOFactory();
    }
    protected final static SampleDTOFactory instance;

    private SampleDTOFactory() {}

    public static SampleDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final SampleDTO toDTO(Sample s) {

        return SampleDTO.newBuilder()
                .setId(s.getId())
                .setHabitatId(s.getHabitat().getId())
                .setTemperature(s.getTemperature())
                .setMaterial(s.getMaterial())
                .setVolume(s.getVolume())
                .setVolumeUnit(s.getVolumeUnit())
                .setCollectiondate(toUnixTimeStamp(s.getCollectionDate()))
                .build();
    }

    @Override
    public final Sample toDB(SampleDTO dto) {
        Sample s = new Sample()
                .setCollectionDate(toDate(dto.getCollectiondate()))
                .setMaterial(dto.getMaterial())
                .setTemperature(dto.getTemperature())
                .setVolume(dto.getVolume())
                .setVolumeUnit(dto.getVolumeUnit());

        if (dto.hasId())
            s.setId(dto.getId());

        return s;
        // cannot set habitat here
    }

    @Override
    public SampleDTOList toDTOList(Iterable<Sample> list) {
        Builder b = SampleDTOList.newBuilder();
        for (Sample o : list) {
            b.addSample(SampleDTOFactory.getInstance().toDTO(o));
        }
        return b.build();
    }
}
