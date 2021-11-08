package de.cebitec.mgx.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public abstract class DBIterator<T> implements AutoCloseableIterator<T> {

    private ResultSet r;
    private Statement s;
    private Connection c;
    private T elem = null;

    public DBIterator(final ResultSet r, final Statement s, final Connection c) {
        this.r = r;
        this.s = s;
        this.c = c;
    }

    @Override
    public boolean hasNext() {
        if (r == null) {
            return false;
        }
        
        if (elem != null) {
            return true;
        }

        try {
            if (r.next()) {
                elem = convert(r);
            } else {
                close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return elem != null;
    }

    @Override
    public T next() {
        T ret = elem;
        elem = null;
        return ret;
    }

    public abstract T convert(ResultSet rs) throws SQLException;

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close() {
        try {
            if (r != null) {
                r.close();
                r = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBIterator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (SQLException ex) {
                }
            }
        }

        try {
            if (s != null) {
                s.close();
                s = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBIterator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (SQLException ex) {
                }
            }
        }

        try {
            if (c != null) {
                c.close();
                c = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DBIterator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException ex) {
                }
            }
        }
    }
}
