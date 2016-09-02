package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Identifiable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class HabitatDAO<T extends Habitat> extends DAO<T> {

    public HabitatDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class<Habitat> getType() {
        return Habitat.class;
    }

    private static final String GET_HABITAT = "SELECT name, altitude, biome, description, latitude, longitude FROM habitat WHERE id=?";

    @Override
    public <T extends Identifiable> T getById(Long id) throws MGXException {
        if (id == null) {
            throw new MGXException("No/Invalid ID supplied.");
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(GET_HABITAT)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Habitat ret = new Habitat();
                        ret.setId(id);
                        ret.setName(rs.getString(1));
                        ret.setAltitude(rs.getInt(2));
                        ret.setBiome(rs.getString(3));
                        ret.setDescription(rs.getString(4));
                        ret.setLatitude(rs.getDouble(5));
                        ret.setLongitude(rs.getDouble(6));
                        return (T) ret;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
    }

}
