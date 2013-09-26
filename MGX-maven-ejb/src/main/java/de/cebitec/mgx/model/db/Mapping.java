package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Mapping")
public class Mapping implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //
    @ManyToOne
    @JoinColumn(name = "run_id", nullable = false)
    protected SeqRun seqrun;
    //
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    protected Job job;
    //
    @ManyToOne
    @JoinColumn(name = "ref_id", nullable = false)
    protected Reference reference;
    //
    @Basic
    @NotNull
    @Column(name = "bam_file", unique = true)
    protected String bam_file;

    @Override
    public Long getId() {
        return id;
    }

    public SeqRun getSeqrun() {
        return seqrun;
    }

    public Job getJob() {
        return job;
    }

    public Reference getReference() {
        return reference;
    }

    public String getBAMFile() {
        return bam_file;
    }
}
