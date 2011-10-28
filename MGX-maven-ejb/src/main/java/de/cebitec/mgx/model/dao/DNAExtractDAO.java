
package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Sample;

/**
 *
 * @author sjaenick
 */
public class DNAExtractDAO<T extends DNAExtract> extends DAO<T> {

    @Override
    Class getType() {
        return DNAExtract.class;
    }

    public Iterable<DNAExtract> bySample(Sample s) {
        return getEntityManager().createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.sample = :sample").
                setParameter("sample", s).getResultList();
    }
}
