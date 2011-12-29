package de.cebitec.mgx.dtoadapter;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.dto.dto.ProjectClassDTO;
import de.cebitec.mgx.dto.dto.ProjectClassDTOList;
import de.cebitec.mgx.dto.dto.RoleDTOList.Builder;
import de.cebitec.mgx.dto.dto.RoleDTOList;

/**
 *
 * @author sjaenick
 */
public class ProjectClassDTOFactory extends DTOConversionBase<ProjectClassI, ProjectClassDTO, ProjectClassDTOList> {

    static {
        instance = new ProjectClassDTOFactory();
    }
    protected final static ProjectClassDTOFactory instance;

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

    @Override
    public ProjectClassDTOList toDTOList(Iterable<ProjectClassI> list) {
        ProjectClassDTOList.Builder ret = ProjectClassDTOList.newBuilder();
        for (ProjectClassI pc : list) {
            ret.addProjectclass(toDTO(pc));
        }
        return ret.build();
    }
}
