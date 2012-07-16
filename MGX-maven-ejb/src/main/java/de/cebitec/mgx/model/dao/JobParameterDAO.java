package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.JobParameter;

/**
 *
 * @author sjaenick
 */
public class JobParameterDAO<T extends JobParameter> extends DAO<T> {

    @Override
    Class getType() {
        return JobParameter.class;
    }

    public Iterable<JobParameter> ByJob(Job j) {
        return getEntityManager().createQuery("SELECT DISTINCT j FROM " + getClassName() + " j WHERE j.job = :job").
                setParameter("job", j).getResultList();
    }
}
