package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.GeneDTOList;
import de.cebitec.mgx.model.db.Gene;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class GeneDTOFactory extends DTOConversionBase<Gene, GeneDTO, GeneDTOList> {
    
    static {
        instance = new GeneDTOFactory();
    }
    protected final static GeneDTOFactory instance;
    
    private GeneDTOFactory() {
    }
    
    public static GeneDTOFactory getInstance() {
        return instance;
    }
    
    @Override
    public final GeneDTO toDTO(Gene h) {
        return GeneDTO.newBuilder()
                .setId(h.getId())
                .setStart(h.getStart())
                .setStop(h.getStop())
                .setCoverage(h.getCoverage())
                .setContigId(h.getContigId())
                .build();
    }
    
    @Override
    public final Gene toDB(GeneDTO dto) {
        Gene h = new Gene();
        h.setStart(dto.getStart());
        h.setStop(dto.getStop());
        h.setCoverage(dto.getCoverage());
        h.setContigId(dto.getContigId());
        
        if (dto.getId() != 0) {
            h.setId(dto.getId());
        }
        
        return h;
    }
    
    @Override
    public GeneDTOList toDTOList(AutoCloseableIterator<Gene> acit) {
        GeneDTOList.Builder b = GeneDTOList.newBuilder();
        try (AutoCloseableIterator<Gene> iter = acit) {
            while (iter.hasNext()) {
                b.addGene(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
