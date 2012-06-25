package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Term")
public class Term implements Identifiable, Serializable {

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

    public Long getParentId() {
        return parent_id;
    }

    public Term setParentId(Long parent_id) {
        this.parent_id = parent_id;
        return this;
    }
}
