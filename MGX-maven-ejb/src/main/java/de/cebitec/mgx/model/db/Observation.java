package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Observation")
public class Observation implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seqid", nullable = false)
    private Sequence seq;
    //
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_id", nullable = false)
    private Attribute attribute;
    //
    @Basic
    @Column(name = "start")
    protected int start;
    @Basic
    @Column(name = "stop")
    protected int stop;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Observation other = (Observation) obj;
        if (this.seq != other.seq && (this.seq == null || !this.seq.equals(other.seq))) {
            return false;
        }
        if (this.attribute != other.attribute && (this.attribute == null || !this.attribute.equals(other.attribute))) {
            return false;
        }
        if (this.start != other.start) {
            return false;
        }
        if (this.stop != other.stop) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.seq != null ? this.seq.hashCode() : 0);
        hash = 89 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
        hash = 89 * hash + this.start;
        hash = 89 * hash + this.stop;
        return hash;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Observation";
    }
}
