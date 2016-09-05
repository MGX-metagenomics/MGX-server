package de.cebitec.mgx.model.db;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "SeqRun")
public class SeqRun implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @NotNull
    protected String name;
    @Basic
    protected String DBFile;
    @Basic
    protected String database_accession;
    @Basic
    @NotNull
    protected Boolean submitted_to_insdc;
    @Basic
    @NotNull
    protected Long sequencing_technology;
    @Basic
    @NotNull
    protected Long sequencing_method;
    @Basic
    @NotNull
    protected Long num_sequences = Long.valueOf(0);
    //
    @OneToMany(mappedBy = "seqrun", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    protected Collection<Sequence> sequences;
    //
    @ManyToOne
    @JoinColumn(name = "dnaextract_id", nullable = false)
    protected DNAExtract dnaextract;
    //
    @OneToMany(mappedBy = "seqrun", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    protected Collection<Job> jobs;

    public DNAExtract getExtract() {
        return dnaextract;
    }

    public SeqRun setExtract(DNAExtract dnaextract) {
        this.dnaextract = dnaextract;
        return this;
    }

    @Override
    public Long getId() {
        return id;
    }

    public SeqRun setId(Long id) {
        this.id = id;
        return this;
    }

    public String getDBFile() {
        return DBFile;
    }

    public SeqRun setDBFile(String DBFile) {
        this.DBFile = DBFile;
        return this;
    }

    public String getName() {
        return name;
    }

    public SeqRun setName(String name) {
        this.name = name;
        return this;
    }

    public String getAccession() {
        return database_accession;
    }

    public SeqRun setAccession(String database_accession) {
        this.database_accession = database_accession;
        return this;
    }
    
    public SeqRun setNumberOfSequences(long num) {
        num_sequences = num;
        return this;
    }

    public Collection<Sequence> getSequences() {
        return sequences;
    }

    public SeqRun setSequences(Collection<Sequence> sequences) {
        this.sequences = sequences;
        return this;
    }

    public Long getSequencingMethod() {
        return sequencing_method;
    }

    public SeqRun setSequencingMethod(Long sequencing_method) {
        this.sequencing_method = sequencing_method;
        return this;
    }

    public Long getSequencingTechnology() {
        return sequencing_technology;
    }

    public SeqRun setSequencingTechnology(Long sequencing_technology) {
        this.sequencing_technology = sequencing_technology;
        return this;
    }

    public Boolean getSubmittedToINSDC() {
        return submitted_to_insdc;
    }

    public SeqRun setSubmittedToINSDC(Boolean submitted_to_insdc) {
        this.submitted_to_insdc = submitted_to_insdc;
        return this;
    }

    public Long getNumberOfSequences() {
        return num_sequences;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SeqRun)) {
            return false;
        }
        SeqRun other = (SeqRun) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.seqrun[id=" + id + "]";
    }

    @PreRemove
    public void removeDBFile() {
        if (DBFile != null) {
            File dbf = new File(DBFile);
            if (dbf.exists()) {
                dbf.delete();
            }
        }
    }
}
