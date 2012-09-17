package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class JobParameterDAO<T extends JobParameter> extends DAO<T> {

    @Override
    Class getType() {
        return JobParameter.class;
    }

    public AutoCloseableIterator<JobParameter> ByJob(Job j) {
        Iterator iterator = getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.job = :job").
                setParameter("job", j).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
