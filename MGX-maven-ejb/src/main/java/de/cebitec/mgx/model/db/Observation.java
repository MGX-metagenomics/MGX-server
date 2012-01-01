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
    @ManyToOne
    @JoinColumn(name = "seqid", nullable = false)
    private Sequence seq;
    //
    @Id
    @ManyToOne
    @JoinColumn(name = "jobid", nullable = false)
    private Job job;
    //
    @Id
    @ManyToOne
    @JoinColumn(name = "attributeid", nullable = false)
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
    public int hashCode() {
        return (int) (seq.getId() + job.getId() + attribute.getId());
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Observation) {
            Observation other = (Observation) object;
            return (other.seq.getId().equals(this.seq.getId())) 
                    && (other.job.getId().equals(this.job.getId())) 
                    && (other.attribute.getId().equals(this.attribute.getId()));
        }
        return false;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.db.Observation";
    }
}
