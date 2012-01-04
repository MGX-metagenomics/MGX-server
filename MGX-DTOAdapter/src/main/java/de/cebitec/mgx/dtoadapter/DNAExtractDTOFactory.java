package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.dto.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.dto.DNAExtractDTO.Builder;
import de.cebitec.mgx.dto.dto.DNAExtractDTOList;

/**
 *
 * @author sjaenick
 */
public class DNAExtractDTOFactory extends DTOConversionBase<DNAExtract, DNAExtractDTO, DNAExtractDTOList> {

    static {
        instance = new DNAExtractDTOFactory();
    }
    protected final static DNAExtractDTOFactory instance;

    private DNAExtractDTOFactory() {
    }

    public static DNAExtractDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final DNAExtractDTO toDTO(DNAExtract d) {
        Builder b = DNAExtractDTO.newBuilder();
        if (d.getId() != null) {
            b.setId(d.getId());
        }
        b = b.setSampleId(d.getSample().getId());

        b = b.setMethod(d.getMethod());
        b = b.setProtocolName(d.getProtocol());

        // optional fields
        if (d.getFivePrimer() != null)
            b = b.setFivePrimePrimer(d.getFivePrimer());
        
        if (d.getThreePrimer() != null)
            b = b.setThreePrimePrimer(d.getThreePrimer());
        
        if (d.getTargetGene() != null)
            b = b.setTargetGene(d.getTargetGene());
        
        if (d.getTargetFragment() != null)
            b = b.setTargetFragment(d.getTargetFragment());
        
        if (d.getDescription() != null)
            b = b.setDescription(d.getDescription());

        return b.build();
    }

    @Override
    public final DNAExtract toDB(DNAExtractDTO dto) {
        DNAExtract d = new DNAExtract();

        if (dto.hasId()) {
            d.setId(dto.getId());
        }

        d.setMethod(dto.getMethod());
        d.setProtocol(dto.getProtocolName());

        // optional fields

        if (dto.hasFivePrimePrimer()) {
            d.setFivePrimer(dto.getFivePrimePrimer());
        }
        if (dto.hasThreePrimePrimer()) {
            d.setThreePrimer(dto.getThreePrimePrimer());
        }
        if (dto.hasTargetGene()) {
            d.setTargetGene(dto.getTargetGene());
        }
        if (dto.hasTargetFragment()) {
            d.setTargetFragment(dto.getTargetFragment());
        }
        if (dto.hasDescription()) {
            d.setDescription(dto.getDescription());
        }

        return d;

    }

    @Override
    public DNAExtractDTOList toDTOList(Iterable<DNAExtract> list) {
        DNAExtractDTOList.Builder b = DNAExtractDTOList.newBuilder();
        for (DNAExtract o : list) {
            b.addExtract(toDTO(o));
        }
        return b.build();
    }
}
