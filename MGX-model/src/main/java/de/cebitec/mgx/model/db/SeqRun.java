package de.cebitec.mgx.model.db;

import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public class SeqRun extends Identifiable {

    private static final long serialVersionUID = 1L;
    protected String name;
    protected String DBFile;
    protected String database_accession;
    protected Boolean submitted_to_insdc;
    protected Long sequencing_technology;
    protected Long sequencing_method;
    protected Long num_sequences = Long.valueOf(0);
    protected Collection<Sequence> sequences;
    protected long dnaextract;
    protected Collection<Job> jobs;

    public long getExtractId() {
        return dnaextract;
    }

    public SeqRun setExtractId(long dnaextract) {
        this.dnaextract = dnaextract;
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
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SeqRun)) {
            return false;
        }
        SeqRun other = (SeqRun) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }

//    @PreRemove
//    public void removeDBFile() {
//        if (DBFile != null) {
//            File dbf = new File(DBFile);
//            if (dbf.exists()) {
//                dbf.delete();
//            }
//        }
//    }
}
