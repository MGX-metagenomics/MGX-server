package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.BinDTOList;
import de.cebitec.mgx.model.db.Bin;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class BinDTOFactory extends DTOConversionBase<Bin, BinDTO, BinDTOList> {

    static {
        instance = new BinDTOFactory();
    }
    protected final static BinDTOFactory instance;

    private BinDTOFactory() {
    }

    public static BinDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final BinDTO toDTO(Bin h) {
        BinDTO.Builder b = BinDTO.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setCompleteness(h.getCompleteness())
                .setContamination(h.getContamination())
                .setTaxonomy(h.getTaxonomy())
                .setN50(h.getN50())
                .setAssemblyId(h.getAssemblyId());

        if (h.getPredictedCDS() > 0) {
            b.setPredictedCds(h.getPredictedCDS());
        }

        return b.build();
    }

    @Override
    public final Bin toDB(BinDTO dto) {
        Bin h = new Bin();
        h.setName(dto.getName());
        h.setCompleteness(dto.getCompleteness());
        h.setContamination(dto.getContamination());
        h.setTaxonomy(dto.getTaxonomy());
        h.setN50(dto.getN50());
        h.setAssemblyId(dto.getAssemblyId());

        if (dto.hasPredictedCds()) {
            h.setPredictedCDS(dto.getPredictedCds());
        }

        if (dto.hasId()) {
            h.setId(dto.getId());
        }

        return h;
    }

    @Override
    public BinDTOList toDTOList(AutoCloseableIterator<Bin> acit) {
        BinDTOList.Builder b = BinDTOList.newBuilder();
        try (AutoCloseableIterator<Bin> iter = acit) {
            while (iter.hasNext()) {
                b.addBin(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
