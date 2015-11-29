package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Attribute")
public class Attribute implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    //
    @Basic
    @NotNull
    @Column(name = "value")
    protected String value;
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attrtype_id", nullable = false)
    protected AttributeType attrtype;
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    protected Job job;
    
    @ManyToOne
    private Attribute parent;
    @OneToMany(mappedBy = "parent")
    private Collection<Attribute> children;

    public Collection<Attribute> getChildren() {
        return children;
    }

    public void setChildren(Collection<Attribute> children) {
        this.children = children;
    }

    public Attribute getParent() {
        return parent;
    }

    public void setParent(Attribute parent) {
        this.parent = parent;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Attribute setId(Long id) {
        this.id = id;
        return this;
    }

    public String getValue() {
        return value;
    }

    public Attribute setValue(String value) {
        this.value = value;
        return this;
    }

    public AttributeType getAttributeType() {
        return attrtype;
    }

    public void setAttributeType(AttributeType attrtype) {
        this.attrtype = attrtype;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Attribute other = (Attribute) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}
