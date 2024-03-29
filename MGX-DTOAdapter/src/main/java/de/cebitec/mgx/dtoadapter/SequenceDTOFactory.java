package de.cebitec.mgx.dtoadapter;

import com.google.protobuf.ByteString;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.dto.dto.SequenceDTOList.Builder;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.seqcompression.FourBitEncoder;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.LimitingIterator;
import java.util.UUID;

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
            b.setSequence(ByteString.copyFrom(FourBitEncoder.encode(a.getSequence().getBytes())));
        }
        return b.build();
    }

    @Override
    public Sequence toDB(SequenceDTO dto) {
        Sequence s = new Sequence();
        s.setName(dto.getName());
        if (dto.getId() != 0) {
            s.setId(dto.getId());
        }
        if (dto.getLength() > 0) {
            s.setLength(dto.getLength());
        }
        if (!dto.getSequence().isEmpty()) {
            s.setSequence(new String(FourBitEncoder.decode(dto.getSequence().toByteArray())));
        }

        return s;
    }

    @Override
    public SequenceDTOList toDTOList(AutoCloseableIterator<Sequence> acit) {
        if (acit instanceof LimitingIterator) {
            return convertLimited((LimitingIterator<Sequence>) acit, UUID.randomUUID().toString());
        } else {
            return convert(acit);
        }
    }
    
    public SequenceDTOList toDTOList(LimitingIterator<Sequence> acit, String uuid) {
        return convertLimited(acit, uuid);
    }

    private SequenceDTOList convert(AutoCloseableIterator<Sequence> acit) {
        Builder b = SequenceDTOList.newBuilder();
        try (AutoCloseableIterator<Sequence> iter = acit) {
            while (iter.hasNext()) {
                b.addSeq(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        b.setComplete(true);

        return b.build();
    }

    private SequenceDTOList convertLimited(LimitingIterator<Sequence> acit, String uuid) {
        Builder b = SequenceDTOList.newBuilder();

        try (LimitingIterator<Sequence> iter = acit) {
            while (iter.hasNext()) {
                b.addSeq(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }

        b.setComplete(!acit.limitReached());
        if (acit.limitReached()) {
            b.setUuid(uuid);
        }
        return b.build();
    }
}
