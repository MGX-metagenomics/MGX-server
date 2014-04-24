package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.MGXMatrixDTO;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.ProfileDTO;
import de.cebitec.mgx.model.misc.Matrix;
import de.cebitec.mgx.model.misc.NamedVector;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class MatrixDTOFactory extends DTOConversionBase<Matrix, MGXMatrixDTO, Object> {

    static {
        instance = new MatrixDTOFactory();
    }
    protected final static MatrixDTOFactory instance;

    private MatrixDTOFactory() {
    }

    public static MatrixDTOFactory getInstance() {
        return instance;
    }

    @Override
    public MGXMatrixDTO toDTO(Matrix a) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix toDB(MGXMatrixDTO dto) {
        Set<NamedVector> data = new HashSet<>();
        for (ProfileDTO pdto : dto.getRowList()) {
            List<Double> ll = pdto.getValues().getValueList();
            double[] tmp = new double[ll.size()];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = ll.get(i);
            }
            data.add(new NamedVector(pdto.getName(), tmp));
        }

        String[] attrNames = null;
        if (dto.hasColNames()) {
            MGXString[] aa = dto.getColNames().getStringList().toArray(new MGXString[]{});
            attrNames = new String[aa.length];
            int i = 0;
            for (MGXString ms : aa) {
                attrNames[i++] = ms.getValue();
            }
        }
        return new Matrix(attrNames, data);
    }

    @Override
    public Object toDTOList(AutoCloseableIterator<Matrix> list) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
