package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTO.Builder;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.global.model.Term;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class TermDTOFactory extends DTOConversionBase<Term, TermDTO, TermDTOList> {

    static {
        instance = new TermDTOFactory();
    }
    protected final static TermDTOFactory instance;

    private TermDTOFactory() {
    }

    public static TermDTOFactory getInstance() {
        return instance;
    }

    @Override
    public TermDTO toDTO(Term a) {
        Builder b = TermDTO.newBuilder()
                .setId(a.getId())
                .setName(a.getName());
        if (a.getParentId() != null) {
            b = b.setParentId(a.getParentId());
        }
        if (a.getDescription() != null) {
            b = b.setDescription(a.getDescription());
        }

        return b.build();
    }

    @Override
    public Term toDB(TermDTO dto) {
        Term t = new Term();
        t.setId(dto.getId());
        t.setName(dto.getName());

        if (dto.hasParentId()) {
            t.setParentId(dto.getParentId());
        }

        if (dto.hasDescription()) {
            t.setDescription(dto.getDescription());
        }

        return t;
    }

    @Override
    public TermDTOList toDTOList(AutoCloseableIterator<Term> acit) {
        TermDTOList.Builder b = TermDTOList.newBuilder();
        try (AutoCloseableIterator<Term> iter = acit) {
            while (iter.hasNext()) {
                b.addTerm(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
