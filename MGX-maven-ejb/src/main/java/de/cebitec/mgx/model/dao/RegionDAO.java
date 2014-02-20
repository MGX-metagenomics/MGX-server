package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.Region;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.Iterator;

/**
 *
 * @author belmann
 */
public class RegionDAO<T extends Region> extends DAO<T> {

    @Override
    Class getType() {
        return Region.class;
    }

    public AutoCloseableIterator<Region> byReference(Reference s) {
        Iterator<Region> iterator = getEntityManager().createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.reference = :reference").
                                    setParameter("reference", s).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
