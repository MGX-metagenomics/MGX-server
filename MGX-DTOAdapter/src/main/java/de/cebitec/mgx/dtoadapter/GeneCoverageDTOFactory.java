/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.GeneCoverageDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.model.db.GeneCoverage;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sj
 */
public class GeneCoverageDTOFactory extends DTOConversionBase<GeneCoverage, GeneCoverageDTO, GeneCoverageDTOList> {

    protected final static GeneCoverageDTOFactory instance = new GeneCoverageDTOFactory();

    private GeneCoverageDTOFactory() {
    }

    public static GeneCoverageDTOFactory getInstance() {
        return instance;
    }

    @Override
    public GeneCoverageDTO toDTO(GeneCoverage a) {
        return GeneCoverageDTO.newBuilder()
                .setRegionId(a.getGeneId())
                .setRunId(a.getRunId())
                .setCoverage(a.getCoverage())
                .build();
    }

    @Override
    public GeneCoverage toDB(GeneCoverageDTO dto) {
        return new GeneCoverage(dto.getCoverage(), dto.getRegionId(), dto.getRunId());
    }

    @Override
    public GeneCoverageDTOList toDTOList(AutoCloseableIterator<GeneCoverage> iter) {
        GeneCoverageDTOList.Builder b = GeneCoverageDTOList.newBuilder();
        while (iter != null && iter.hasNext()) {
            b.addGeneCoverage(toDTO(iter.next()));
        }
        return b.build();
    }

}
