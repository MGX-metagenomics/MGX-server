package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.AttributeDTO;
import de.cebitec.mgx.dto.dto.AttributeDTO.Builder;
import de.cebitec.mgx.dto.dto.AttributeDTOList;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.util.AutoCloseableIterator;

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
                .setJobId(attr.getJobId())
                .setAttributeTypeId(attr.getAttributeTypeId());
        if (attr.getParentId()!= Identifiable.INVALID_IDENTIFIER) {
            b.setParentId(attr.getParentId());
        }
        return b.build();
    }

    public AttributeDTO toDTO(Attribute attr, long parentId) {
        Builder b = AttributeDTO.newBuilder();
        b.setId(attr.getId())
                .setValue(attr.getValue())
                .setJobId(attr.getJobId())
                .setAttributeTypeId(attr.getAttributeTypeId());
        b.setParentId(parentId);
        return b.build();
    }

    @Override
    public final Attribute toDB(AttributeDTO dto) {
        Attribute a = new Attribute()
                .setValue(dto.getValue());

        
        if (dto.getId() != 0) {
            a.setId(dto.getId());
        }
        return a;
    }

    @Override
    public AttributeDTOList toDTOList(AutoCloseableIterator<Attribute> acit) {
        AttributeDTOList.Builder b = AttributeDTOList.newBuilder();
        try (AutoCloseableIterator<Attribute> iter = acit) {
            while (iter.hasNext()) {
                b.addAttribute(toDTO(iter.next()));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return b.build();
    }
}
