
package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "AttributeType")
public class AttributeType implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Basic
    @NotNull
    @Column(name = "name")
    protected String name;
    
    @Basic
    @NotNull
    @Column(name="value_type")
    protected String value_type;
    
    @OneToMany(mappedBy = "attrtype", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    protected Collection<Attribute> attributes;

    @Override
    public Long getId() {
        return id;
    }

    public AttributeType setId(Long id) {
        this.id = id;
        return this;
    }

    public String getValueType() {
        return value_type;
    }

    public AttributeType setValueType(String value_type) {
        this.value_type = value_type;
        return this;
    }

    public String getName() {
        return name;
    }

    public AttributeType setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AttributeType)) {
            return false;
        }
        AttributeType other = (AttributeType) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Attribute[id=" + id + "]";
    }
}
