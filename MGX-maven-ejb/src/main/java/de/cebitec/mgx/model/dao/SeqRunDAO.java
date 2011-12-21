package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    public void delete(Long id) throws MGXException {
        SeqRun sr = getById(id);
        if (sr != null) {
            // remove persistent storage file
            String dBFile = sr.getDBFile();
            if (dBFile != null) {
                SeqReaderFactory.delete(dBFile);
            }

            /*
             * JPA CascadeType.DELETE fetches and delete all entities individually;
             * we can do better by manually deleting all associated reads ..
             */
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = getConnection();
                stmt = conn.prepareStatement("DELETE FROM read WHERE seqrun_id=?");
                stmt.setLong(1, id);
                stmt.execute();
            } catch (SQLException ex) {
            } finally {
                close(conn, stmt, null);
            }

            // remove persistent entity
            EntityManager e = getEntityManager();
            e.remove(sr);
            e.flush();
        }

        super.delete(id);
    }

    public Iterable<SeqRun> byDNAExtract(DNAExtract h) {
        return getEntityManager().createQuery("SELECT DISTINCT s FROM " + getClassName() + " s WHERE s.dnaextract = :extract").
                setParameter("extract", h).getResultList();
    }
}
