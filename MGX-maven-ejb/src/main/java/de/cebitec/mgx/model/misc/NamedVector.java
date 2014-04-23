
package de.cebitec.mgx.model.misc;

/**
 *
 * @author sjaenick
 */
public class NamedVector {

    private final String name;
    private final long[] data;

    public NamedVector(final String name, final long[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public long[] getData() {
        return data;
    }

}
