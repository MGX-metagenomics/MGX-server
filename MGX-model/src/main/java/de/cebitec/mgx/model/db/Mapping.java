package de.cebitec.mgx.model.db;

/**
 *
 * @author sjaenick
 */
public class Mapping extends Identifiable {

    private static final long serialVersionUID = 1L;
    protected long seqrun;
    protected long job;
    protected long reference;
    protected String bam_file;

    public long getSeqRunId() {
        return seqrun;
    }

    public void setSeqrunId(long seqrun) {
        this.seqrun = seqrun;
    }

    public void setJobId(long job) {
        this.job = job;
    }

    public void setReferenceId(long reference) {
        this.reference = reference;
    }

    public void setBAMFile(String bam_file) {
        this.bam_file = bam_file;
    }

    public long getJobId() {
        return job;
    }

    public long getReferenceId() {
        return reference;
    }

    public String getBAMFile() {
        return bam_file;
    }
}
