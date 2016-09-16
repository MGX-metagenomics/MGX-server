package de.cebitec.mgx.model.db;

import java.io.Serializable;

/**
 *
 * @author sjaenick
 */
public abstract class Identifiable implements Serializable {

    public static long INVALID_IDENTIFIER = -1;
    private long id = INVALID_IDENTIFIER;

    public final long getId() {
        return id;
    }

    public final void setId(long id) {
        this.id = id;
    }

    @Override
    public final String toString() {
        return getClass().getName() + "[id=" + id + "]";
    }
}
