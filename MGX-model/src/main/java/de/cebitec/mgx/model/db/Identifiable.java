package de.cebitec.mgx.model.db;

/**
 *
 * @author sjaenick
 */
public abstract class Identifiable {

    public static long INVALID_IDENTIFIER = -1;
    private long id = INVALID_IDENTIFIER;

    public final long getId() {
        return id;
    }

    public final void setId(long id) {
        if (id > 0) {
            this.id = id;
        }
    }

    @Override
    public final String toString() {
        return getClass().getName() + "[id=" + id + "]";
    }
}
