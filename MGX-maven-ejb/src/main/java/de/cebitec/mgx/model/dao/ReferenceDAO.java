
package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Reference;

/**
 *
 * @author sj
 */
public class ReferenceDAO<T extends Reference> extends DAO<T> {

    @Override
    public Class getType() {
        return Reference.class;
    }
    
}
