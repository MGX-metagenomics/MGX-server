package de.cebitec.mgx.model.db;

import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public class Attribute extends Identifiable {

    private static final long serialVersionUID = 1L;

    protected String value;
    protected long attrtype;
    protected long job;
    private long parent = INVALID_IDENTIFIER;
    private Collection<Attribute> children;

    public Collection<Attribute> getChildren() {
        return children;
    }

    public void setChildren(Collection<Attribute> children) {
        this.children = children;
    }

    public long getParentId() {
        return parent;
    }

    public void setParentId(long parent) {
        this.parent = parent;
    }

    public String getValue() {
        return value;
    }

    public Attribute setValue(String value) {
        this.value = value;
        return this;
    }

    public long getAttributeTypeId() {
        return attrtype;
    }

    public void setAttributeTypeId(long attrtype) {
        this.attrtype = attrtype;
    }

    public long getJobId() {
        return job;
    }

    public void setJobId(long job) {
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
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER)
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }
}
