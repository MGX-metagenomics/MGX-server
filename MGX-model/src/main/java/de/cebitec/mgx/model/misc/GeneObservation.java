package de.cebitec.mgx.model.misc;

/**
 *
 * @author sjaenick
 */
public class GeneObservation {

    private final int start;
    private final int stop;
    private final String attributeName;
    private final String attributeTypeName;

    public GeneObservation(int start, int stop, String attributeName, String attributeTypeName) {
        this.start = start;
        this.stop = stop;
        this.attributeName = attributeName;
        this.attributeTypeName = attributeTypeName;
    }

    public int getStart() {
        return start;
    }

    public int getStop() {
        return stop;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeTypeName() {
        return attributeTypeName;
    }
}
