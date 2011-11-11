package de.cebitec.mgx.dtoadapter;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.dto.ProjectClassDTO;
import de.cebitec.mgx.dto.RoleDTOList;
import de.cebitec.mgx.dto.RoleDTOList.Builder;

/**
 *
 * @author sjaenick
 */
public class ProjectClassDTOFactory extends DTOConversionBase<ProjectClassI, ProjectClassDTO> {

    static {
        instance = new ProjectClassDTOFactory();
    }
    protected static ProjectClassDTOFactory instance;

    private ProjectClassDTOFactory() {
    }

    public static ProjectClassDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ProjectClassDTO toDTO(ProjectClassI pc) {
        Builder roles = RoleDTOList.newBuilder();
        for (RoleI r : pc.getRoles()) {
            roles.addRole(RoleDTOFactory.getInstance().toDTO(r));
        }
        return ProjectClassDTO.newBuilder()
                .setName(pc.getName())
                .setRoles(roles.build())
                .build();
    }

    @Override
    public ProjectClassI toDB(ProjectClassDTO dto) {
        // not used
        return null;
    }
}
