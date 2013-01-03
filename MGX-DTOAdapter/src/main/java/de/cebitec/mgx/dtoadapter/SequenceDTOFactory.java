package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class SequenceDTOFactory extends DTOConversionBase<Sequence, SequenceDTO, SequenceDTOList> {

    static {
        instance = new SequenceDTOFactory();
    }
    protected final static SequenceDTOFactory instance;

    private SequenceDTOFactory() {
    }

    public static SequenceDTOFactory getInstance() {
        return instance;
    }

    @Override
    public SequenceDTO toDTO(Sequence a) {
        SequenceDTO.Builder b = SequenceDTO.newBuilder()
                .setName(a.getName());
        if (a.getId() != Identifiable.INVALID_IDENTIFIER) {
            b.setId(a.getId());
        }
        if (a.getLength() != -1) {
            b.setLength(a.getLength());
        }
        if (a.getSequence() != null) {
            b.setSequence(a.getSequence());
        }
        return b.build();
    }

    @Override
    public Sequence toDB(SequenceDTO dto) {
        Sequence s = new Sequence();
        s.setName(dto.getName());
        if (dto.hasId()) {
            s.setId(dto.getId());
        }
        if (dto.hasLength()) {
            s.setLength(dto.getLength());
        }
        if (dto.hasSequence()) {
            s.setSequence(dto.getSequence());
        }

        return s;
    }

    @Override
    public SequenceDTOList toDTOList(AutoCloseableIterator<Sequence> acit) {
        Builder b = SequenceDTOList.newBuilder();
        try (AutoCloseableIterator<Sequence> iter = acit) {
            while (iter.hasNext()) {
                b.addSeq(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
