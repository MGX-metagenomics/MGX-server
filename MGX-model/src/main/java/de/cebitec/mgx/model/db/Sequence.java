
package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Read")
@Cacheable(false)
public class Sequence implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(name = "name", nullable = false)
    protected String name;
    //
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "seqrun_id", nullable = false)
    protected SeqRun seqrun;
    
    @Basic
    @Column(name="length", nullable= false)
    protected int len = -1;
    
    @Basic
    @Column(name = "discard", nullable=false)
    protected boolean discard = false;
    
    @Transient
    protected String sequence;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLength() {
        return len;
    }

    public void setLength(int len) {
        this.len = len;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Sequence)) {
            return false;
        }
        Sequence other = (Sequence) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.ejb.Sequence[id=" + id + "]";
    }
}