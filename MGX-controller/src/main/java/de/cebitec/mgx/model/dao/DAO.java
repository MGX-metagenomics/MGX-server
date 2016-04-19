package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Identifiable;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public abstract class DAO<T extends Identifiable> {

    private final MGXControllerImpl ctx;

    public DAO(MGXControllerImpl ctx) {
        this.ctx = ctx;
    }
    
//    public void setController(MGXControllerImpl ctx) {
//        this.ctx = ctx;
//    }

    public final MGXController getController() {
        return ctx;
    }

    public EntityManager getEntityManager() {
        return ctx.getEntityManager();
    }

    public Connection getConnection() throws SQLException {
        return ctx.getConnection();
    }

    abstract Class getType();

    @SuppressWarnings("unchecked")
    public <T extends Identifiable> T getById(Long id) throws MGXException {
        if (id == null) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        Class<T> type = getType();
        T ret = (T) getEntityManager().<T>find(type, id);
        if (ret == null) {
            throw new MGXException("No object of type " + getType().getSimpleName() + " for ID " + id + ".");
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<T> getByIds(Collection<Long> ids) throws MGXException {
        Iterator<T> it = getEntityManager()
                .<T>createQuery("SELECT DISTINCT o FROM " + getClassName() + " o WHERE o.id IN :ids", getType())
                .setParameter("ids", ids)
                .getResultList()
                .iterator();
        return new ForwardingIterator<>(it);
    }

    @SuppressWarnings("unchecked")
    public AutoCloseableIterator<T> getAll() throws MGXException {
        Iterator<T> iterator = getEntityManager().<T>createQuery("SELECT DISTINCT o FROM " + getClassName() + " o", getType())
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
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    public void update(T obj) throws MGXException {
        EntityManager e = getEntityManager();
        try {
            e.merge(obj);
            e.flush();
        } catch (PersistenceException ex) {
            throw new MGXException(ex);
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
