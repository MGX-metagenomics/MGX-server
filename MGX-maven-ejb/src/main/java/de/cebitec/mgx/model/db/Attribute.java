package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Attribute")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="attr_class", length=1, discriminatorType= DiscriminatorType.CHAR)
@DiscriminatorValue("B")
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

    public AttributeType getAttrType() {
        return attrtype;
    }

    public void setAttrType(AttributeType attrtype) {
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
