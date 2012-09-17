package de.cebitec.mgx.dtoadapter;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.mgx.dto.dto.MembershipDTO;
import de.cebitec.mgx.dto.dto.MembershipDTOList;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class MembershipDTOFactory extends DTOConversionBase<MembershipI, MembershipDTO, MembershipDTOList> {

    static {
        instance = new MembershipDTOFactory();
    }
    protected final static MembershipDTOFactory instance;

    private MembershipDTOFactory() {
    }

    public static MembershipDTOFactory getInstance() {
        return instance;
    }

    @Override
    public MembershipDTO toDTO(MembershipI m) {
        return MembershipDTO.newBuilder()
                .setProject(ProjectDTOFactory.getInstance().toDTO(m.getProject()))
                .setRole(RoleDTOFactory.getInstance().toDTO(m.getRole()))
                .build();
    }

    @Override
    public MembershipI toDB(MembershipDTO dto) {
        // not used
        return null;
    }

    @Override
    public MembershipDTOList toDTOList(AutoCloseableIterator<MembershipI> acit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
