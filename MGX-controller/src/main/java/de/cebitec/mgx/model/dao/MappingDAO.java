package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Mapping;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.io.File;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class MappingDAO<T extends Mapping> extends DAO<T> {

    public MappingDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Mapping.class;
    }

    public AutoCloseableIterator<Mapping> bySeqRun(SeqRun sr) throws MGXException {
        Iterator<Mapping> iterator = getEntityManager().<Mapping>createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.seqrun = :run", Mapping.class).
                setParameter("run", sr).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }

    public AutoCloseableIterator<Mapping> byReference(Reference ref) throws MGXException {
        Iterator<Mapping> iterator = getEntityManager().<Mapping>createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.reference = :ref", Mapping.class).
                setParameter("ref", ref).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }

    @Override
    public void delete(long id) throws MGXException {
        final Mapping mapping = getById(id);
        File bamFile = new File(mapping.getBAMFile());
        if (bamFile.exists()) {
            bamFile.delete();
        }
        super.delete(id);
    }
}
