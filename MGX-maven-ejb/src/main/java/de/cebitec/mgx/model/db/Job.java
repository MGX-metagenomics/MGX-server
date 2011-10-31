package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Job")
public class Job implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //
    @ManyToOne
    @JoinColumn(name = "seqrun_id", nullable = false)
    protected SeqRun seqrun;
    //
    @ManyToOne
    @JoinColumn(name = "tool_id", nullable = false)
    protected Tool tool;
    @Basic
    protected String created_by;
    //
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true, insertable = false)
    protected Date startDate = null;
    //
    @Column(nullable = true, insertable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date finishDate = null;
    //
    @Basic
    @Column(name = "parameters")
    protected String parameters;
    @Basic
    @Column(name = "job_state")
    private int jobstate;
//    // not persistent, mapped by its integer value
//    @Transient
//    protected JobState status;
//
//    @PrePersist
//    @PreUpdate
//    void copyJobState() {
//        jobstate = status.ordinal();
//    }
//
//    @PostLoad
//    @PostUpdate
//    void translateJobState() {
//        status = JobState.values()[jobstate];
//    }

    @Override
    public Long getId() {
        return id;
    }

    public Job setId(Long id) {
        this.id = id;
        return this;
    }

    public JobState getStatus() {
        return JobState.values()[jobstate];
    }

    public Job setStatus(JobState status) {
        jobstate = status.getValue();
        return this;
    }

    public Tool getTool() {
        return tool;
    }

    public Job setTool(Tool tool) {
        this.tool = tool;
        return this;
    }

    public String getParameters() {
        return parameters;
    }

    public Job setParameters(String parameters) {
        this.parameters = parameters;
        return this;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public Job setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Job setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public SeqRun getSeqrun() {
        return seqrun;
    }

    public Job setSeqrun(SeqRun seqrun) {
        this.seqrun = seqrun;
        return this;
    }

    public String getCreator() {
        return created_by;
    }

    public Job setCreator(String created_by) {
        this.created_by = created_by;
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
        if (!(object instanceof Job)) {
            return false;
        }
        Job other = (Job) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Job[id=" + id + "]";
    }
}
