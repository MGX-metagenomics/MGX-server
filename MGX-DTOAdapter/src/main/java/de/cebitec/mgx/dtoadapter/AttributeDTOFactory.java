package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.model.db.Attribute;

/**
 *
 * @author sjaenick
 */
public class AttributeDTOFactory extends DTOConversionBase<Attribute, AttributeDTO, AttributeDTOList> {
    
    static {
        instance = new AttributeDTOFactory();
    }
    
    protected final static AttributeDTOFactory instance;
    
    @Override
    public final AttributeDTO toDTO(Attribute a) {
        return AttributeDTO.newBuilder()
                .setType(a.getType())
                .build();
    }

    @Override
    public final Attribute toDB(AttributeDTO dto) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AttributeDTOList toDTOList(Iterable<Attribute> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
