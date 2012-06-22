package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTO.Builder;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.model.db.Term;

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
        if (a.getParentId() != -1)
            b = b.setParentId(a.getParentId());
        if (a.getDescription() != null) 
            b = b.setDescription(a.getDescription());
        
        return b.build();
                
    }

    @Override
    public Term toDB(TermDTO dto) {
        Term t = new Term();
        t.setId(dto.getId())
                .setName(dto.getName());
        
        if (dto.hasParentId())
            t.setParentId(dto.getParentId());
        
        if (dto.hasDescription())
            t.setDescription(dto.getDescription());
        
        return t;
    }

    @Override
    public TermDTOList toDTOList(Iterable<Term> list) {
        TermDTOList.Builder b = TermDTOList.newBuilder();
        for (Term t: list) {
            b.addTerm(toDTO(t));
        }
        return b.build();
    }
}
