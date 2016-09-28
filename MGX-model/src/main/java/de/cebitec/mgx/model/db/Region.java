package de.cebitec.mgx.model.db;

/**
 *
 * @author belmann
 */
public class Region extends Identifiable {

    private String name;
    private String description;
    private String type;
    private long ref_id;
    private int start;
    private int stop;

    public long getReferenceId() {
        return ref_id;
    }

    public void setReferenceId(long reference) {
        this.ref_id = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }
}
