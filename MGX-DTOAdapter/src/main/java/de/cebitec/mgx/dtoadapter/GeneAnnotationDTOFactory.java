/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.GeneAnnotationDTO;
import de.cebitec.mgx.dto.dto.GeneAnnotationDTOList;
import de.cebitec.mgx.model.db.GeneAnnotation;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sj
 */
public class GeneAnnotationDTOFactory extends DTOConversionBase<GeneAnnotation, GeneAnnotationDTO, GeneAnnotationDTOList> {

    protected final static GeneAnnotationDTOFactory instance = new GeneAnnotationDTOFactory();

    private GeneAnnotationDTOFactory() {
    }

    public static GeneAnnotationDTOFactory getInstance() {
        return instance;
    }

    @Override
    public GeneAnnotationDTO toDTO(GeneAnnotation a) {
        return GeneAnnotationDTO.newBuilder()
                .setGeneId(a.getGeneId())
                .setAttributeId(a.getAttributeId())
                .setStart(a.getStart())
                .setStop(a.getStop())
                .build();
    }

    @Override
    public GeneAnnotation toDB(GeneAnnotationDTO dto) {
        return new GeneAnnotation(dto.getGeneId(), dto.getAttributeId(), dto.getStart(), dto.getStop());
    }

    @Override
    public GeneAnnotationDTOList toDTOList(AutoCloseableIterator<GeneAnnotation> iter) {
        GeneAnnotationDTOList.Builder b = GeneAnnotationDTOList.newBuilder();
        while (iter != null && iter.hasNext()) {
            b.addAnnotation(toDTO(iter.next()));
        }
        return b.build();
    }

    public List<GeneAnnotation> toList(GeneAnnotationDTOList dtolist) {
        List<GeneAnnotation> ret = new ArrayList<>();
        for (GeneAnnotationDTO dto : dtolist.getAnnotationList()) {
            ret.add(toDB(dto));
        }
        return ret;
    }

}
