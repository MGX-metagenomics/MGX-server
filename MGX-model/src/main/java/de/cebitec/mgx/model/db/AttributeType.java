
package de.cebitec.mgx.model.db;

import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public class AttributeType extends Identifiable {
    
    public static final char VALUE_NUMERIC = 'N';
    public static final char VALUE_DISCRETE = 'D';
    //
    public static final char STRUCTURE_BASIC = 'B';
    public static final char STRUCTURE_HIERARCHICAL = 'H';
    
            
    private static final long serialVersionUID = 1L;

    protected String name;
    protected String value_type;
    protected String structure;
    protected Collection<Attribute> attributes;

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
         hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AttributeType)) {
            return false;
        }
        AttributeType other = (AttributeType) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }
}
