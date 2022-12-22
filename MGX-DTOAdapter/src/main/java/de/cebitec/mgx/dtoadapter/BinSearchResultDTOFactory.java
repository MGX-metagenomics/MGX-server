package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.BinSearchResultDTO;
import de.cebitec.mgx.dto.dto.BinSearchResultDTOList;
import de.cebitec.mgx.model.misc.BinSearchResult;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class BinSearchResultDTOFactory extends DTOConversionBase<BinSearchResult, BinSearchResultDTO, BinSearchResultDTOList> {
    
    static {
        instance = new BinSearchResultDTOFactory();
    }
    protected final static BinSearchResultDTOFactory instance;
    
    private BinSearchResultDTOFactory() {
    }
    
    public static BinSearchResultDTOFactory getInstance() {
        return instance;
    }
    
    @Override
    public final BinSearchResultDTO toDTO(BinSearchResult h) {
        return BinSearchResultDTO.newBuilder()
                .setContigId(h.getContigId())
                .setContigName(h.getContigName())
                .setRegionId(h.getRegionId())
                .setAttributeName(h.getAttributeName())
                .setAttributeTypeValue(h.getAttributeTypeValue())
                .build();
    }
    
    @Override
    public final BinSearchResult toDB(BinSearchResultDTO dto) {
        return new BinSearchResult(dto.getContigId(), dto.getContigName(),
                dto.getRegionId(),
                dto.getAttributeName(), dto.getAttributeTypeValue());
    }
    
    @Override
    public BinSearchResultDTOList toDTOList(AutoCloseableIterator<BinSearchResult> acit) {
        BinSearchResultDTOList.Builder b = BinSearchResultDTOList.newBuilder();
        try (AutoCloseableIterator<BinSearchResult> iter = acit) {
            while (iter.hasNext()) {
                b.addResult(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
