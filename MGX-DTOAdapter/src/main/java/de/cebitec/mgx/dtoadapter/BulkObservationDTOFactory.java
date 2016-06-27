package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.BulkObservationDTO;
import de.cebitec.mgx.dto.dto.BulkObservationDTOList;
import de.cebitec.mgx.model.misc.BulkObservation;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class BulkObservationDTOFactory extends DTOConversionBase<BulkObservation, BulkObservationDTO, BulkObservationDTOList> {

    protected final static BulkObservationDTOFactory instance = new BulkObservationDTOFactory();

    private BulkObservationDTOFactory() {
    }

    public static BulkObservationDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final BulkObservationDTO toDTO(BulkObservation bo) {
        return BulkObservationDTO.newBuilder()
                .setSeqrunId(bo.getSeqRunId())
                .setSeqName(bo.getSequenceName())
                .setAttributeId(bo.getAttributeId())
                .setStart(bo.getStart())
                .setStop(bo.getStop())
                .build();
    }

    @Override
    public final BulkObservation toDB(BulkObservationDTO dto) {
        return new BulkObservation(dto.getSeqrunId(), dto.getSeqName(),
                dto.getAttributeId(), dto.getStart(), dto.getStop());
    }

    public final List<BulkObservation> toDBList(BulkObservationDTOList bol) {
        long runId = -1;
        String seqName = null;
        long attrId = -1;
        int start = -1;
        int stop = -1;

        List<BulkObservation> ret = new ArrayList<>(bol.getBulkObservationCount());

        //
        // sparse dto, only changes are present
        //
        for (BulkObservationDTO boDTO : bol.getBulkObservationList()) {

            if (boDTO.hasSeqrunId()) {
                runId = boDTO.getSeqrunId();
            }

            if (boDTO.hasSeqName()) {
                seqName = boDTO.getSeqName();
            }

            if (boDTO.hasAttributeId()) {
                attrId = boDTO.getAttributeId();
            }

            if (boDTO.hasStart()) {
                start = boDTO.getStart();
            }

            if (boDTO.hasStop()) {
                stop = boDTO.getStop();
            }

            BulkObservation bo = new BulkObservation(runId, seqName, attrId, start, stop);
            ret.add(bo);
        }
        return ret;
    }

    @Override
    public BulkObservationDTOList toDTOList(AutoCloseableIterator<BulkObservation> list) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
