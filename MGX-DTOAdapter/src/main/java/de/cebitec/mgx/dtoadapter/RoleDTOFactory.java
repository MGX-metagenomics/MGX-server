package de.cebitec.mgx.dtoadapter;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.dto.RoleDTO;

/**
 *
 * @author sjaenick
 */
public class RoleDTOFactory extends DTOConversionBase<RoleI, RoleDTO> {

    static {
        instance = new RoleDTOFactory();
    }
    protected static RoleDTOFactory instance;

    private RoleDTOFactory() {
    }

    public static RoleDTOFactory getInstance() {
        return instance;
    }

    @Override
    public RoleDTO toDTO(RoleI r) {
        return RoleDTO.newBuilder()
                .setName(r.getName())
                .build();
    }

    @Override
    public RoleI toDB(RoleDTO dto) {
        // not used
        return null;
    }
}
