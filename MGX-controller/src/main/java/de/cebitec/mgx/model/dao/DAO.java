package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Identifiable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public abstract class DAO<T extends Identifiable> {

    private final MGXController ctx;

    public DAO(MGXController ctx) {
        this.ctx = ctx;
    }

    protected final MGXController getController() {
        return ctx;
    }

    protected final Connection getConnection() throws SQLException {
        return ctx.getConnection();
    }

    abstract Class getType();
    
    public abstract T getById(long id) throws MGXException;
    
    public abstract long create(T obj) throws MGXException;

//    @SuppressWarnings("unchecked")
//    public <T extends Identifiable> T getById(Long id) throws MGXException {
//        if (id == null) {
//            throw new MGXException("No/Invalid ID supplied.");
//        }
//        Class<T> type = getType();
//        T ret = (T) getEntityManager().<T>find(type, id);
//        if (ret == null) {
//            throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
//        }
//        return ret;
//    }

//    @SuppressWarnings("unchecked")
//    public AutoCloseableIterator<T> getByIds(Collection<Long> ids) throws MGXException {
//        Iterator<T> it = getEntityManager()
//                .<T>createQuery("SELECT DISTINCT o FROM " + getClassName() + " o WHERE o.id IN :ids", getType())
//                .setParameter("ids", ids)
//                .getResultList()
//                .iterator();
//        return new ForwardingIterator<>(it);
//    }

//    @SuppressWarnings("unchecked")
//    public AutoCloseableIterator<T> getAll() throws MGXException {
//        Iterator<T> iterator = getEntityManager().<T>createQuery("SELECT DISTINCT o FROM " + getClassName() + " o", getType())
//                .getResultList()
//                .iterator();
//        return new ForwardingIterator<>(iterator);
//    }
//    public long create(T obj) throws MGXException {
//        EntityManager e = getEntityManager();
//        try {
//            e.persist(obj);
//            e.flush();
//        } catch (PersistenceException ex) {
//            throw new MGXException(ex);
//        }
//        return obj.getId();
//    }

//    public void update(T obj) throws MGXException {
//        if (obj.getId() == null) {
//            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
//        }
//        EntityManager e = getEntityManager();
//        try {
//            e.merge(obj);
//            e.flush();
//        } catch (PersistenceException ex) {
//            throw new MGXException(ex);
//        }
//    }
//    public abstract void delete(long id) throws MGXException;
//    public void delete(long id) throws MGXException {
//        T obj = getById(id);
//        if (obj != null) {
//            EntityManager e = getEntityManager();
//            e.remove(obj);
//            e.flush();
//        } else {
//            throw new MGXException("Cannot delete non-existing object");
//        }
//    }
    protected final String getClassName() {
        return getType().getSimpleName();
    }

    public void dispose() {
    }

    protected final void close(Connection c, Statement s, ResultSet r) {
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

    protected final static String toSQLTemplateString(int size) {
        if (size == 0) {
            return "";
        }

        StringBuilder oBuilder = new StringBuilder("?");
        for (int i = 2; i <= size; i++) {
            oBuilder.append(",?");
        }

        return oBuilder.toString();
    }
}
