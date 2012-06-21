package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.TermDTO;
import de.cebitec.mgx.dto.dto.TermDTOList;
import de.cebitec.mgx.dto.dto.TermDTOList.Builder;
import de.cebitec.mgx.model.db.Term;


/**
 *
 * @author sjaenick
 */
public class TermDTOFactory extends DTOConversionBase<Term, TermDTO, TermDTOList>{

    public static TermDTOFactory getInstance() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public TermDTO toDTO(Term a) {
        TermDTO.Builder b = TermDTO.newBuilder()
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
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TermDTOList toDTOList(Iterable<Term> list) {
        Builder b = TermDTOList.newBuilder();
        for (Term t : list) {
            b.addTerm(toDTO(t));
        }
        return b.build();
    }
    
}
