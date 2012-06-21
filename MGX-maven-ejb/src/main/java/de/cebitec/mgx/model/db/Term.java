package de.cebitec.mgx.model.db;

/**
 *
 * @author sjaenick
 */
public class Term implements Identifiable {

    private long id = -1;
    private long parent_id = -1;
    private String name;
    private String description;

    @Override
    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getParentId() {
        return parent_id;
    }

    public void setParentId(long parent_id) {
        this.parent_id = parent_id;
    }
}
