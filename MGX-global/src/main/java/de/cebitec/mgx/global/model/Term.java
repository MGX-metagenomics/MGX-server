package de.cebitec.mgx.global.model;

import de.cebitec.mgx.model.db.Identifiable;

/**
 *
 * @author sjaenick
 */
public class Term extends Identifiable {

    private static final long serialVersionUID = 1L;
    private long parent_id = INVALID_IDENTIFIER;
    private String name;
    private String description;

    public String getDescription() {
        return description;
    }

    public Term setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public Term setName(String name) {
        this.name = name;
        return this;
    }

    public Long getParentId() {
        return parent_id;
    }

    public Term setParentId(Long parent_id) {
        this.parent_id = parent_id;
        return this;
    }
}
