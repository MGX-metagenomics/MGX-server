package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Sample;

/**
 *
 * @author sjaenick
 */
public class SampleDAO<T extends Sample> extends DAO<T> {

    @Override
    Class getType() {
        return Sample.class;
    }

    public Iterable<Sample> byHabitat(Habitat h) {
        return getEntityManager().createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.habitat = :hab").
                setParameter("hab", h).getResultList();
    }
}
