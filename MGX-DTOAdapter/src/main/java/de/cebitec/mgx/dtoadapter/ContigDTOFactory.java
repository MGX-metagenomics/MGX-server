package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.ContigDTOList;
import de.cebitec.mgx.model.db.Contig;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class ContigDTOFactory extends DTOConversionBase<Contig, ContigDTO, ContigDTOList> {
    
    static {
        instance = new ContigDTOFactory();
    }
    protected final static ContigDTOFactory instance;
    
    private ContigDTOFactory() {
    }
    
    public static ContigDTOFactory getInstance() {
        return instance;
    }
    
    @Override
    public final ContigDTO toDTO(Contig h) {
        return ContigDTO.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setGc(h.getGC())
                .setLengthBp(h.getLength())
                .setCoverage(h.getCoverage())
                .setBinId(h.getBinId())
                .build();
    }
    
    @Override
    public final Contig toDB(ContigDTO dto) {
        Contig h = new Contig();
        h.setGC(dto.getGc());
        h.setLength(dto.getLengthBp());
        h.setCoverage(dto.getCoverage());
        h.setName(dto.getName());
        h.setBinId(dto.getBinId());
        
        if (dto.hasId()) {
            h.setId(dto.getId());
        }
        
        return h;
    }
    
    @Override
    public ContigDTOList toDTOList(AutoCloseableIterator<Contig> acit) {
        ContigDTOList.Builder b = ContigDTOList.newBuilder();
        try (AutoCloseableIterator<Contig> iter = acit) {
            while (iter.hasNext()) {
                b.addContig(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
