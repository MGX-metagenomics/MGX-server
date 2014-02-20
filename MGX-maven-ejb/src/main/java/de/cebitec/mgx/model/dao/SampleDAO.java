package de.cebitec.mgx.model.dao;

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

    @Override
    Class getType() {
        return Sample.class;
    }

    public AutoCloseableIterator<Sample> byHabitat(Habitat h) {
        Iterator<Sample> iterator = getEntityManager().createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.habitat = :hab").
                                    setParameter("hab", h).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
