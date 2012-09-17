
package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class DNAExtractDAO<T extends DNAExtract> extends DAO<T> {

    @Override
    Class getType() {
        return DNAExtract.class;
    }

    public AutoCloseableIterator<DNAExtract> bySample(Sample s) {
        Iterator iterator = getEntityManager().createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.sample = :sample").
                                    setParameter("sample", s).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
