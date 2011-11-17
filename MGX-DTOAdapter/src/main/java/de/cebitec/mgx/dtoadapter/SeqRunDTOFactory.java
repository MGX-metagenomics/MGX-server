package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.model.db.SeqRun;

/**
 *
 * @author sjaenick
 */
public class SeqRunDTOFactory extends DTOConversionBase<SeqRun, SeqRunDTO> {

    static {
        instance = new SeqRunDTOFactory();
    }
    protected static SeqRunDTOFactory instance;

    private SeqRunDTOFactory() {}

    public static SeqRunDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final SeqRunDTO toDTO(SeqRun s) {
        return SeqRunDTO.newBuilder()
                .setId(s.getId())
                .setAccession(s.getAccession())
                .setExtractId(s.getExtract().getId())
                .build();
    }

    @Override
    public final SeqRun toDB(SeqRunDTO dto) {
        SeqRun s = new SeqRun()
                .setAccession(dto.getAccession());

        if (dto.hasId())
            s.setId(dto.getId());

        return s;
        // cannot set sample here
    }
}
