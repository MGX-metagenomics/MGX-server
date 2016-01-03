//package de.cebitec.mgx.model.db;
//
//import java.util.Collection;
//import javax.persistence.*;
//
///**
// *
// * @author sjaenick
// */
//@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@DiscriminatorValue("H")
//public class HierarchicalAttribute extends Attribute {
//
//    private static final long serialVersionUID = 1L;
//    @ManyToOne
//    private HierarchicalAttribute parent;
//    @OneToMany(mappedBy = "parent")
//    private Collection<HierarchicalAttribute> children;
//
//    public Collection<HierarchicalAttribute> getChildren() {
//        return children;
//    }
//
//    public void setChildren(Collection<HierarchicalAttribute> children) {
//        this.children = children;
//    }
//
//    public HierarchicalAttribute getParent() {
//        return parent;
//    }
//
//    public void setParent(HierarchicalAttribute parent) {
//        this.parent = parent;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final HierarchicalAttribute other = (HierarchicalAttribute) obj;
//        if (this.parent != other.parent && (this.parent == null || !this.parent.equals(other.parent))) {
//            return false;
//        }
//        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 7;
//        hash = 41 * hash + (this.parent != null ? this.parent.hashCode() : 0);
//        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
//        return hash;
//    }
//
//    @Override
//    public String toString() {
//        return "de.cebitec.mgx.model.db.HierarchicalAttribute[id=" + id + "]";
//    }
//}