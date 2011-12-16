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
                .setExtractId(s.getExtract().getId())
                .setAccession(s.getAccession())
                .setSubmittedToInsdc(s.getSubmittedToINSDC())
                .setSequencingMethod(s.getSequencingMethod())
                .setSequencingTechnology(s.getSequencingTechnology())
                .build();
    }

    @Override
    public final SeqRun toDB(SeqRunDTO dto) {
        SeqRun s = new SeqRun()
                .setSubmittedToINSDC(dto.getSubmittedToInsdc())
                .setSequencingMethod(dto.getSequencingMethod())
                .setSequencingTechnology(dto.getSequencingTechnology());
        
        if (dto.getSubmittedToInsdc())
            s.setAccession(dto.getAccession());

        if (dto.hasId())
            s.setId(dto.getId());

        return s;
        // cannot set sample here
    }
}
