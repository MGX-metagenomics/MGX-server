package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.model.db.Habitat;

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

}
