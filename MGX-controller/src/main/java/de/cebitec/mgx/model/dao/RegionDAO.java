package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
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

    public RegionDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Region.class;
    }

    public AutoCloseableIterator<Region> byReference(Reference s) throws MGXException {
        Iterator<Region> iterator = getEntityManager().createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.reference = :reference", Region.class).
                setParameter("reference", s).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
