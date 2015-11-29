
package de.cebitec.mgx.statistics.data;

/**
 *
 * @author sjaenick
 */
public class NamedVector {

    private final String name;
    private final double[] data;

    public NamedVector(final String name, final double[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public double[] getData() {
        return data;
    }

}
