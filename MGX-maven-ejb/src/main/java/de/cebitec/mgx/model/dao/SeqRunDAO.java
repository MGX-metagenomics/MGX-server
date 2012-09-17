package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import javax.persistence.EntityManager;

/**
 *
 * @author sjaenick
 */
public class SeqRunDAO<T extends SeqRun> extends DAO<T> {

    @Override
    Class getType() {
        return SeqRun.class;
    }

    @Override
    public void delete(long id) throws MGXException {
        final SeqRun sr = getById(id);
        // remove persistent storage file
        String dBFile = sr.getDBFile();
        if (dBFile != null) {
            SeqReaderFactory.delete(dBFile);
        }

        // FIXME move remainder to background thread
        Connection conn = null;
        PreparedStatement stmt = null;

        /*
         * JPA CascadeType.DELETE fetches and delete all entities individually;
         * we can do better by manually deleting all referring objects ..
         */
        
        try {
            conn = getConnection();

            // delete observations
            stmt = conn.prepareStatement("DELETE FROM observation WHERE seq_id IN (SELECT id FROM read WHERE seqrun_id=?)");
            stmt.setLong(1, id);
            stmt.execute();

            // delete attributecounts
            stmt = conn.prepareStatement("DELETE FROM attributecount WHERE attr_id IN "
                    + "(SELECT id FROM attribute WHERE job_id IN "
                    + "(SELECT id from job WHERE seqrun_id=?)"
                    + ")");
            stmt.setLong(1, id);
            stmt.execute();

            // delete attributes
            stmt = conn.prepareStatement("DELETE FROM attribute WHERE job_id IN "
                    + "(SELECT id from job WHERE seqrun_id=?)");
            stmt.setLong(1, id);
            stmt.execute();
            // delete jobs
            //            stmt = conn.prepareStatement("DELETE FROM job WHERE seqrun_id=?");
            //            stmt.setLong(1, id);
            //            stmt.execute();
            Iterator<Job> iter = getController().getJobDAO().BySeqRun(sr);
            while (iter.hasNext()) {
                getController().getJobDAO().delete(iter.next().getId());
            }

            // delete reads
            stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?");
            stmt.setLong(1, id);
            stmt.execute();
        } catch (SQLException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            close(conn, stmt, null);
        }

        // remove persistent entity
        EntityManager e = getEntityManager();
        e.remove(sr);
        e.flush();
    }

    public AutoCloseableIterator<SeqRun> byDNAExtract(DNAExtract extract) {
        Iterator iterator = getEntityManager().createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.dnaextract = :extract").
                                    setParameter("extract", extract).getResultList().iterator();
        return new ForwardingIterator<>(iterator);
    }
}
