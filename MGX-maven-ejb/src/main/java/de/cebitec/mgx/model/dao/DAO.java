package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public abstract class DAO<T extends Identifiable> {

    private MGXControllerImpl ctx;
    private EntityManager em;

    public void setController(MGXControllerImpl ctx) {
        this.ctx = ctx;
        this.em = ctx.getEntityManager();
    }

    public MGXController getController() {
        return ctx;
    }

    public void setEntityManager(EntityManager e) {
        this.em = e;
    }

    public EntityManager getEntityManager() {
        if (ctx != null) {
            return ctx.getEntityManager();
        } else {
            return em;
        }
    }

    public Connection getConnection() {
        if (ctx != null) {
            // MGX application
            return ctx.getConnection();
        } else {
            // MGX global
            // FIXME find better way to obtain connection

            // hibernate 3
            //return getEntityManager().unwrap(Session.class).connection();

            // hibernate 4
            SessionFactoryImpl impl = (SessionFactoryImpl) getEntityManager().unwrap(Session.class).getSessionFactory();
            try {
                return impl.getConnectionProvider().getConnection();
            } catch (SQLException ex) {
                Logger.getLogger(DAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }

    abstract Class getType();

    public T getById(Long id) throws MGXException {
        if (id == null) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        T ret = (T) getEntityManager().find(getType(), id);
        if (ret == null) {
            throw new MGXException("No object of type " + getType() + " for ID " + id + ".");
        }
        return ret;
    }

    public AutoCloseableIterator<T> getByIds(Collection<Long> ids) throws MGXException {
        Iterator<T> it = getEntityManager()
                .createQuery("SELECT DISTINCT o FROM " + getClassName() + " o WHERE o.id IN :ids", getType())
                .setParameter("ids", ids)
                .getResultList()
                .iterator();
        return new ForwardingIterator<>(it);
    }

    public AutoCloseableIterator<T> getAll() {
        Iterator<T> iterator = getEntityManager().createQuery("SELECT DISTINCT o FROM " + getClassName() + " o", getType())
                                    .getResultList()
                                    .iterator();
        return new ForwardingIterator<>(iterator);
    }

    public long create(T obj) throws MGXException {
        EntityManager e = getEntityManager();
        try {
            e.persist(obj);
            e.flush();
        } catch (PersistenceException ex) {
            throw new MGXException(ex.getCause());
        }
        return obj.getId();
    }

    public void update(T obj) throws MGXException {
        EntityManager e = getEntityManager();
        try {
            e.merge(obj);
            e.flush();
        } catch (PersistenceException ex) {
            throw new MGXException(ex.getCause());
        }
    }

    public void delete(long id) throws MGXException {
        T obj = getById(id);
        if (obj != null) {
            EntityManager e = getEntityManager();
            e.remove(obj);
            e.flush();
        } else {
            throw new MGXException("Cannot delete non-existing object");
        }
    }

    final protected String getClassName() {
        return getType().getSimpleName();
    }

    protected void close(Connection c, Statement s, ResultSet r) {
        try {
            if (r != null) {
                r.close();
                r = null;
            }
            if (s != null) {
                s.close();
                s = null;
            }
            if (c != null) {
                c.close();
                c = null;
            }
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (s != null) {
                    s.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    /*
     * from http://snippets.dzone.com/posts/show/91
     */
    protected static String join(Iterable< ? extends Object> pColl, String separator) {
        Iterator< ? extends Object> oIter;
        if (pColl == null || (!(oIter = pColl.iterator()).hasNext())) {
            return "";
        }
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext()) {
            oBuilder.append(separator).append(oIter.next());
        }
        return oBuilder.toString();
    }

    protected static String toSQLTemplateString(List l) {
        if ((l == null) || (l.isEmpty())) {
            return "";
        }

        StringBuilder oBuilder = new StringBuilder("?");
        int size = l.size();
        for (int i = 2; i <= size; i++) {
            oBuilder.append(",?");
        }

        return oBuilder.toString();
    }
}
