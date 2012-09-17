package de.cebitec.mgx.dtoadapter;

import de.cebitec.gpms.core.ProjectI;
import de.cebitec.mgx.dto.dto.ProjectDTO;
import de.cebitec.mgx.dto.dto.ProjectDTOList;
import de.cebitec.mgx.util.AutoCloseableIterator;

/**
 *
 * @author sjaenick
 */
public class ProjectDTOFactory extends DTOConversionBase<ProjectI, ProjectDTO, ProjectDTOList> {

    static {
        instance = new ProjectDTOFactory();
    }
    protected final static ProjectDTOFactory instance;

    private ProjectDTOFactory() {
    }

    public static ProjectDTOFactory getInstance() {
        return instance;
    }

    @Override
    public ProjectDTO toDTO(ProjectI p) {
        return ProjectDTO.newBuilder()
                .setName(p.getName())
                .setProjectClass(ProjectClassDTOFactory.getInstance().toDTO(p.getProjectClass()))
                .build();
    }

    @Override
    public ProjectI toDB(ProjectDTO dto) {
        // not used
        return null;
    }

    @Override
    public ProjectDTOList toDTOList(AutoCloseableIterator<ProjectI> acit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
