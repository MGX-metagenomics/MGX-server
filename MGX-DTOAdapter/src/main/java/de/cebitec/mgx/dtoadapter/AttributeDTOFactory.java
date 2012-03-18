package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTO.Builder;
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

    public static AttributeDTOFactory getInstance() {
        return instance;
    }

    @Override
    public AttributeDTO toDTO(Attribute attr) {
        Builder b = AttributeDTO.newBuilder();
        b.setId(attr.getId())
                .setValue(attr.getValue())
                .setJobid(attr.getJob().getId())
                .setAttrTypeId(attr.getAttributeType().getId());
        if (attr.getParent() != null) {
            b.setParentId(attr.getParent().getId());
        }
        return b.build();
    }

    @Override
    public final Attribute toDB(AttributeDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public AttributeDTOList toDTOList(Iterable<Attribute> list) {
        AttributeDTOList.Builder b = AttributeDTOList.newBuilder();
        for (Attribute attr : list) {
            b.addAttribute(toDTO(attr));
        }
        return b.build();
    }
}
