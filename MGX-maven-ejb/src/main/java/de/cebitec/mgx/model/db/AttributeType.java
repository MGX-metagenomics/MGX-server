
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
@Table(name = "AttributeType")
public class AttributeType implements Serializable, Identifiable {
    
    public static final char VALUE_NUMERIC = 'N';
    public static final char VALUE_DISCRETE = 'D';
    //
    public static final char STRUCTURE_BASIC = 'B';
    public static final char STRUCTURE_HIERARCHICAL = 'H';
    
            
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
    @Column(name="value_type", length=1, columnDefinition="CHARACTER")
    protected String value_type;
    
    @Basic
    @NotNull
    @Column(name="structure", length=1, columnDefinition="CHARACTER")
    protected String structure;
    
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

    public char getValueType() {
        return value_type.charAt(0);
    }

    public AttributeType setValueType(char value_type) {
        this.value_type = String.valueOf(value_type);
        return this;
    }

    public char getStructure() {
        return structure.charAt(0);
    }

    public AttributeType setStructure(char structure) {
        this.structure = String.valueOf(structure);
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
        return "de.cebitec.mgx.model.db.AttributeType[id=" + id + "]";
    }
}
