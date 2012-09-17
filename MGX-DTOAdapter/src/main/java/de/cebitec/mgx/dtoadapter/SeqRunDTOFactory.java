package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SeqRunDTO.Builder;
import de.cebitec.mgx.dto.dto.SeqRunDTOList;
import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Term;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class SeqRunDTOFactory extends DTOConversionBase<SeqRun, SeqRunDTO, SeqRunDTOList> {

    static {
        instance = new SeqRunDTOFactory();
    }
    protected final static SeqRunDTOFactory instance;
    protected static MGXGlobal global;

    private SeqRunDTOFactory() {
    }

    public static SeqRunDTOFactory getInstance(MGXGlobal g) {
        global = g;
        return instance;
    }

    @Override
    public final SeqRunDTO toDTO(SeqRun s) {
        Term seqMethod = null;
        Term seqTech = null;

        try {
            seqMethod = global.getTermDAO().getById(s.getSequencingMethod());
            seqTech = global.getTermDAO().getById(s.getSequencingTechnology());
        } catch (MGXException ex) {
            Logger.getLogger(SeqRunDTOFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        TermDTO techDTO = TermDTOFactory.getInstance().toDTO(seqTech);
        TermDTO methDTO = TermDTOFactory.getInstance().toDTO(seqMethod);

        Builder b = SeqRunDTO.newBuilder()
                .setId(s.getId())
                .setName(s.getName())
                .setExtractId(s.getExtract().getId())
                .setSubmittedToInsdc(s.getSubmittedToINSDC())
                .setSequencingMethod(methDTO)
                .setSequencingTechnology(techDTO)
                .setNumSequences(s.getNumberOfSequences());

        if (s.getSubmittedToINSDC()) {
            b.setAccession(s.getAccession());
        }

        return b.build();
    }

    @Override
    public final SeqRun toDB(SeqRunDTO dto) {
        SeqRun s = new SeqRun()
                .setName(dto.getName())
                .setSubmittedToINSDC(dto.getSubmittedToInsdc())
                .setSequencingMethod(dto.getSequencingMethod().getId())
                .setSequencingTechnology(dto.getSequencingTechnology().getId());

        if (dto.getSubmittedToInsdc()) {
            s.setAccession(dto.getAccession());
        }

        if (dto.hasId()) {
            s.setId(dto.getId());
        }

        return s;
        // cannot set sample here
    }

    @Override
    public SeqRunDTOList toDTOList(AutoCloseableIterator<SeqRun> acit) {
        SeqRunDTOList.Builder b = SeqRunDTOList.newBuilder();
        try (AutoCloseableIterator<SeqRun> iter = acit) {
            while (iter.hasNext()) {
                b.addSeqrun(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
