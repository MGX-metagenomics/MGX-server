
package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
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

    public DNAExtractDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return DNAExtract.class;
    }

    public AutoCloseableIterator<DNAExtract> bySample(Sample s) throws MGXException {
        Iterator<DNAExtract> iterator = getEntityManager().<DNAExtract>createQuery("SELECT DISTINCT d FROM " + getClassName() + " d WHERE d.sample = :sample", DNAExtract.class).
                                    setParameter("sample", s).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
