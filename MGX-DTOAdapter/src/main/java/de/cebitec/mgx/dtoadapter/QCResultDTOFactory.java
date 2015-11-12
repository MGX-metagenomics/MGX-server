/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.DataRowDTO;
import de.cebitec.mgx.dto.dto.QCResultDTO;
import de.cebitec.mgx.dto.dto.QCResultDTOList;
import de.cebitec.mgx.qc.DataRowI;
import de.cebitec.mgx.qc.QCResult;
import de.cebitec.mgx.qc.QCResultI;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class QCResultDTOFactory extends DTOConversionBase<QCResultI, QCResultDTO, QCResultDTOList> {

    static {
        instance = new QCResultDTOFactory();
    }
    protected final static QCResultDTOFactory instance;

    private QCResultDTOFactory() {
    }

    public static QCResultDTOFactory getInstance() {
        return instance;
    }

    @Override
    public QCResultDTO toDTO(QCResultI a) {
        QCResultDTO.Builder b = QCResultDTO.newBuilder();
        b.setName(a.getName());
        b.setDescription(a.getDescription());
        for (DataRowI dr : a.getData()) {
            DataRowDTO.Builder drdb = DataRowDTO.newBuilder();
            drdb.setName(dr.getName());
            for (float f : dr.getData()) {
                drdb.addValue(f);
            }
            b.addRow(drdb.build());
        }
        return b.build();
    }

    @Override
    public QCResult toDB(QCResultDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public QCResultDTOList toDTOList(AutoCloseableIterator<QCResultI> list) {
        QCResultDTOList.Builder b = QCResultDTOList.newBuilder();
        while (list.hasNext()) {
            b = b.addResult(toDTO(list.next()));
        }
        return b.build();
    }

}
