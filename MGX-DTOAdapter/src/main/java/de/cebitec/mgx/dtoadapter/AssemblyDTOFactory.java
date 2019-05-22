package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.AssemblyDTO;
import de.cebitec.mgx.dto.dto.AssemblyDTOList;
import de.cebitec.mgx.dto.dto.AssemblyDTOList.Builder;
import de.cebitec.mgx.model.db.Assembly;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class AssemblyDTOFactory extends DTOConversionBase<Assembly, AssemblyDTO, AssemblyDTOList> {
    
    static {
        instance = new AssemblyDTOFactory();
    }
    protected final static AssemblyDTOFactory instance;
    
    private AssemblyDTOFactory() {
    }
    
    public static AssemblyDTOFactory getInstance() {
        return instance;
    }
    
    @Override
    public final AssemblyDTO toDTO(Assembly h) {
        return AssemblyDTO.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setAssemblyJobId(h.getAsmjobId())
                .setReadsAssembled(h.getReadsAssembled())
                .setTotalLengthBp(h.getTotalLengthBp())
                .build();
    }
    
    @Override
    public final Assembly toDB(AssemblyDTO dto) {
        Assembly h = new Assembly();
        h.setName(dto.getName());
        h.setAsmjobId(dto.getAssemblyJobId());
        h.setReadsAssembled(dto.getReadsAssembled());
        h.setTotalLengthBp(dto.getTotalLengthBp());
        
        if (dto.hasId()) {
            h.setId(dto.getId());
        }
        
        return h;
    }
    
    @Override
    public AssemblyDTOList toDTOList(AutoCloseableIterator<Assembly> acit) {
        Builder b = AssemblyDTOList.newBuilder();
        try (AutoCloseableIterator<Assembly> iter = acit) {
            while (iter.hasNext()) {
                b.addAssembly(toDTO(iter.next()));
            }
        } catch (Exception ex) {
        }
        return b.build();
    }
}
