package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.PCAResultDTO;
import de.cebitec.mgx.model.misc.PCAResult;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.Point;

/**
 *
 * @author sjaenick
 */
public class PCAResultDTOFactory extends DTOConversionBase<PCAResult, PCAResultDTO, Object> {

    static {
        instance = new PCAResultDTOFactory();
    }
    protected final static PCAResultDTOFactory instance;

    private PCAResultDTOFactory() {
    }

    public static PCAResultDTOFactory getInstance() {
        return instance;
    }

    @Override
    public PCAResultDTO toDTO(PCAResult pca) {
        PCAResultDTO.Builder b = PCAResultDTO.newBuilder();
        for (double d : pca.getVariances()) {
            b.addVariance(d);
        }

        for (Point p : pca.getLoadings()) {
            b.addLoading(PointDTOFactory.getInstance().toDTO(p));
        }

        for (Point p : pca.getDatapoints()) {
            b.addDatapoint(PointDTOFactory.getInstance().toDTO(p));
        }
        return b.build();
    }

    @Override
    public PCAResult toDB(PCAResultDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Object toDTOList(AutoCloseableIterator<PCAResult> list) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
