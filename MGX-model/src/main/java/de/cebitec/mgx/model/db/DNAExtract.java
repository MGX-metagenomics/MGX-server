package de.cebitec.mgx.model.db;


/**
 *
 * @author sjaenick
 */
public class DNAExtract extends Identifiable {

    private String name;
    protected String method;
    protected String protocol;
    protected String fivePrimer;
    protected String threePrimer;
    protected String targetGene;
    protected String targetFragment;
    protected String description;
    //
    protected long sample;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getFivePrimer() {
        return fivePrimer;
    }

    public DNAExtract setFivePrimer(String fivePrimer) {
        this.fivePrimer = fivePrimer;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public DNAExtract setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public DNAExtract setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getTargetFragment() {
        return targetFragment;
    }

    public DNAExtract setTargetFragment(String targetFragment) {
        this.targetFragment = targetFragment;
        return this;
    }

    public String getTargetGene() {
        return targetGene;
    }

    public DNAExtract setTargetGene(String targetGene) {
        this.targetGene = targetGene;
        return this;
    }

    public String getThreePrimer() {
        return threePrimer;
    }

    public DNAExtract setThreePrimer(String threePrimer) {
        this.threePrimer = threePrimer;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DNAExtract setDescription(String description) {
        this.description = description;
        return this;
    }

    public long getSampleId() {
        return sample;
    }
    
    public void setSampleId(long sample) {
        this.sample = sample;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DNAExtract)) {
            return false;
        }
        DNAExtract other = (DNAExtract) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }
}
