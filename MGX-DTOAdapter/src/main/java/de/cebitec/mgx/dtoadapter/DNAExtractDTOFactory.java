package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.DNAExtractDTO;
import de.cebitec.mgx.dto.dto.DNAExtractDTO.Builder;
import de.cebitec.mgx.dto.dto.DNAExtractDTOList;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.util.AutoCloseableIterator;

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
        if (d.getId() != DNAExtract.INVALID_IDENTIFIER) {
            b.setId(d.getId());
        }
        b = b.setSampleId(d.getSampleId());
        b = b.setName(d.getName());
        b = b.setMethod(d.getMethod());
        b = b.setProtocolName(d.getProtocol());

        // optional fields
        if (d.getFivePrimer() != null) {
            b = b.setFivePrimePrimer(d.getFivePrimer());
        }

        if (d.getThreePrimer() != null) {
            b = b.setThreePrimePrimer(d.getThreePrimer());
        }

        if (d.getTargetGene() != null) {
            b = b.setTargetGene(d.getTargetGene());
        }

        if (d.getTargetFragment() != null) {
            b = b.setTargetFragment(d.getTargetFragment());
        }

        if (d.getDescription() != null) {
            b = b.setDescription(d.getDescription());
        }

        return b.build();
    }

    @Override
    public final DNAExtract toDB(DNAExtractDTO dto) {
        DNAExtract d = new DNAExtract();

        if (dto.getId() != 0) {
            d.setId(dto.getId());
        }
        
        d.setSampleId(dto.getSampleId());

        d.setName(dto.getName());
        d.setMethod(dto.getMethod());
        d.setProtocol(dto.getProtocolName());

        // optional fields

        if (!dto.getFivePrimePrimer().isEmpty()) {
            d.setFivePrimer(dto.getFivePrimePrimer());
        }
        if (!dto.getThreePrimePrimer().isEmpty()) {
            d.setThreePrimer(dto.getThreePrimePrimer());
        }
        if (!dto.getTargetGene().isEmpty()) {
            d.setTargetGene(dto.getTargetGene());
        }
        if (!dto.getTargetFragment().isEmpty()) {
            d.setTargetFragment(dto.getTargetFragment());
        }
        if (!dto.getDescription().isEmpty()) {
            d.setDescription(dto.getDescription());
        }

        return d;
    }

    @Override
    public DNAExtractDTOList toDTOList(AutoCloseableIterator<DNAExtract> acit) {
        DNAExtractDTOList.Builder b = DNAExtractDTOList.newBuilder();
        try (AutoCloseableIterator<DNAExtract> iter = acit) {
            while (iter.hasNext()) {
                b.addExtract(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
