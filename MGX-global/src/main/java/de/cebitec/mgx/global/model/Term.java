package de.cebitec.mgx.global.model;

import de.cebitec.mgx.model.db.Identifiable;
import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
public class Term implements Identifiable, Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long parent_id;
    private String name;
    private String description;

    @Override
    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Term setDescription(String description) {
        this.description = description;
        return this;
    }

    public Term setId(Long id) {
        this.id = id;
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
