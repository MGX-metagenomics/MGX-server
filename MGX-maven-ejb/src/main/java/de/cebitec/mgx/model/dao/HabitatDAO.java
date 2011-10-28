package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Habitat;

/**
 *
 * @author sjaenick
 */
public class HabitatDAO<T extends Habitat> extends DAO<T> {

    @Override
    Class getType() {
        return Habitat.class;
    }

}
