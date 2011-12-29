package de.cebitec.mgx.dtoadapter;

import de.cebitec.mgx.dto.dto.ToolDTO;
import de.cebitec.mgx.dto.dto.ToolDTOList;
import de.cebitec.mgx.model.db.Tool;

/**
 *
 * @author sjaenick
 */
public class ToolDTOFactory extends DTOConversionBase<Tool, ToolDTO, ToolDTOList> {

    static {
        instance = new ToolDTOFactory();
    }
    protected final static ToolDTOFactory instance;

    private ToolDTOFactory() {}

    public static ToolDTOFactory getInstance() {
        return instance;
    }

    @Override
    public final ToolDTO toDTO(Tool s) {
        return ToolDTO.newBuilder()
                .setId(s.getId())
                .setName(s.getName())
                .setDescription(s.getDescription())
                .setVersion(s.getVersion())
                .setAuthor(s.getAuthor())
                .setUrl(s.getUrl())
                .build();
    }

    public final Tool toDB(ToolDTO dto, boolean copyID) {
        Tool t = new Tool()
                .setName(dto.getName())
                .setDescription(dto.getDescription())
                .setVersion(dto.getVersion())
                .setAuthor(dto.getAuthor())
                .setUrl(dto.getUrl());

        if (copyID && dto.hasId()) {
            t.setId(dto.getId());
        }
        return t;
    }

    @Override
    public Tool toDB(ToolDTO dto) {
        return toDB(dto, true);
    }

    @Override
    public ToolDTOList toDTOList(Iterable<Tool> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
