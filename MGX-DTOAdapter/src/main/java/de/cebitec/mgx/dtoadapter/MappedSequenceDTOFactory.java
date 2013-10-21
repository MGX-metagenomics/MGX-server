package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.MappedSequenceDTO;
import de.cebitec.mgx.dto.dto.MappedSequenceDTOList;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.MappedSequence;

/**
 *
 * @author sj
 */
public class MappedSequenceDTOFactory extends DTOConversionBase<MappedSequence, MappedSequenceDTO, MappedSequenceDTOList> {

    static {
        instance = new MappedSequenceDTOFactory();
    }
    protected final static MappedSequenceDTOFactory instance;

    private MappedSequenceDTOFactory() {
    }

    public static MappedSequenceDTOFactory getInstance() {
        return instance;
    }

    @Override
    public MappedSequenceDTO toDTO(MappedSequence a) {
        return MappedSequenceDTO.newBuilder()
                .setSeqId(a.getSeqId())
                .setStart(a.getStart())
                .setStop(a.getStop())
                .setIdentity(a.getIdentity())
                .build();
    }

    @Override
    public MappedSequence toDB(MappedSequenceDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public MappedSequenceDTOList toDTOList(AutoCloseableIterator<MappedSequence> it) {
        MappedSequenceDTOList.Builder b = MappedSequenceDTOList.newBuilder();
        try (AutoCloseableIterator<MappedSequence> iter = it) {
            while (iter.hasNext()) {
                b.addMappedSequence(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }

}
