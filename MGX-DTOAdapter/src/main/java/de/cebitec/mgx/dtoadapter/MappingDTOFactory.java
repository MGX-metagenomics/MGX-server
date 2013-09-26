package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.MappingDTO;
import de.cebitec.mgx.dto.dto.MappingDTOList;
import de.cebitec.mgx.dto.dto.MappingDTOList.Builder;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class MappingDTOFactory extends DTOConversionBase<Mapping, MappingDTO, MappingDTOList> {

    static {
        instance = new MappingDTOFactory();
    }
    protected final static MappingDTOFactory instance;

    private MappingDTOFactory() {
    }

    public static MappingDTOFactory getInstance() {
        return instance;
    }

    @Override
    public MappingDTO toDTO(Mapping a) {
        return MappingDTO.newBuilder()
                .setId(a.getId())
                .setJobId(a.getJob().getId())
                .setReferenceId(a.getReference().getId())
                .setRunId(a.getSeqrun().getId())
                .build();
    }

    @Override
    public Mapping toDB(MappingDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public MappingDTOList toDTOList(AutoCloseableIterator<Mapping> acit) {
        Builder b = MappingDTOList.newBuilder();
        try (AutoCloseableIterator<Mapping> iter = acit) {
            while (iter.hasNext()) {
                b.addMapping(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
