package de.cebitec.mgx.model.db;

import javax.persistence.Basic;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
public class Term implements Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    private Long parent_id;
    @Basic
    @NotNull
    private String name;
    @Basic
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

    public long getParentId() {
        return parent_id;
    }

    public Term setParentId(long parent_id) {
        this.parent_id = parent_id;
        return this;
    }
}
