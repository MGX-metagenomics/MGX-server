package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.AttributeTypeDTO;
import de.cebitec.mgx.dto.dto.AttributeTypeDTO.Builder;
import de.cebitec.mgx.dto.dto.AttributeTypeDTOList;
import de.cebitec.mgx.model.db.AttributeType;

/**
 *
 * @author sjaenick
 */
public class AttributeTypeDTOFactory extends DTOConversionBase<AttributeType, AttributeTypeDTO, AttributeTypeDTOList> {

    static {
        instance = new AttributeTypeDTOFactory();
    }
    protected final static AttributeTypeDTOFactory instance;

    public static AttributeTypeDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final AttributeTypeDTO toDTO(AttributeType a) {
        String valueType = String.valueOf(a.getValueType());
        Builder b = AttributeTypeDTO.newBuilder()
                .setId(a.getId())
                .setName(a.getName())
                .setStructure(String.valueOf(a.getStructure()))
                .setValueType(valueType);
        return b.build();
    }

    @Override
    public final AttributeType toDB(AttributeTypeDTO dto) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public AttributeTypeDTOList toDTOList(Iterable<AttributeType> list) {
        AttributeTypeDTOList.Builder b = AttributeTypeDTOList.newBuilder();
        for (AttributeType at : list) {
            b.addAttributeType(toDTO(at));
        }
        return b.build();
    }
}