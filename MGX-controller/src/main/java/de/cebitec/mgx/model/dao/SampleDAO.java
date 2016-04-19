package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class SampleDAO<T extends Sample> extends DAO<T> {

    public SampleDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Sample.class;
    }

    public AutoCloseableIterator<Sample> byHabitat(Habitat h) throws MGXException {
        Iterator<Sample> iterator = getEntityManager().<Sample>createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.habitat = :hab", Sample.class).
                setParameter("hab", h).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
