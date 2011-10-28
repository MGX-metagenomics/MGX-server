package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.dto.DNAExtractDTO;
/**
 *
 * @author sjaenick
 */
public class DNAExtractDTOFactory extends DTOConversionBase<DNAExtract, DNAExtractDTO> {

    static {
        instance = new DNAExtractDTOFactory();
    }
    protected static DNAExtractDTOFactory instance;

    private DNAExtractDTOFactory() {}

    public static DNAExtractDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final DNAExtractDTO toDTO(DNAExtract d) {
        return DNAExtractDTO.newBuilder()
                .setId(d.getId())
                .setSampleId(d.getSample().getId())
                .build();
    }

    @Override
    public final DNAExtract toDB(DNAExtractDTO dto) {
        DNAExtract d = new DNAExtract();

        if (dto.hasId())
            d.setId(dto.getId());

        return d;
    }
}
