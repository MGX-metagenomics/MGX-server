package de.cebitec.mgx.model.dao;

import de.cebitec.gpms.util.GPMSManagedConnectionI;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Identifiable;
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

    protected final GPMSManagedConnectionI getConnection() throws SQLException {
        return ctx.getConnection();
    }

    abstract Class<T> getType();
    
    public abstract T getById(long id) throws MGXException;
    
    public abstract long create(T obj) throws MGXException;

    protected final String getClassName() {
        return getType().getSimpleName();
    }

    public void dispose() {
    }

    protected final void close(GPMSManagedConnectionI c, Statement s, ResultSet r) {
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
