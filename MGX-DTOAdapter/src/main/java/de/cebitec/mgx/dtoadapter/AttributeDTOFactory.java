package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.AttributeDTO;
import de.cebitec.mgx.model.db.Attribute;

/**
 *
 * @author sjaenick
 */
public class AttributeDTOFactory extends DTOConversionBase<Attribute, AttributeDTO> {

    @Override
    public final AttributeDTO toDTO(Attribute a) {
        return AttributeDTO.newBuilder().setType(a.getType()).build();
    }

    @Override
    public final Attribute toDB(AttributeDTO dto) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
