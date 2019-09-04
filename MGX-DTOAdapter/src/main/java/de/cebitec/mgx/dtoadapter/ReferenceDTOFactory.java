package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ReferenceDTO;
import de.cebitec.mgx.dto.dto.ReferenceDTOList;
import de.cebitec.mgx.dto.dto.ReferenceDTOList.Builder;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class ReferenceDTOFactory extends DTOConversionBase<Reference, ReferenceDTO, ReferenceDTOList> {

    static {
        instance = new ReferenceDTOFactory();
    }
    protected final static ReferenceDTOFactory instance;

    private ReferenceDTOFactory() {
    }

    public static ReferenceDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ReferenceDTO toDTO(Reference r) {
        return ReferenceDTO.newBuilder()
                .setId(r.getId())
                .setName(r.getName())
                .setLength(r.getLength())
                .build();
    }

    @Override
    public Reference toDB(ReferenceDTO dto) {
        Reference r = new Reference();
        if (dto.getId() != 0) {
            r.setId(dto.getId());
        }
        r.setName(dto.getName());
        r.setLength(dto.getLength());
        return r;
    }

    @Override
    public ReferenceDTOList toDTOList(AutoCloseableIterator<Reference> acit) {
        Builder b = ReferenceDTOList.newBuilder();
        try (AutoCloseableIterator<Reference> iter = acit) {
            while (iter.hasNext()) {
                b.addReference(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
