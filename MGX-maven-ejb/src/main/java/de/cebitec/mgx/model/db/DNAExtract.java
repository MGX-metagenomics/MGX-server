package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "DNAExtract")
public class DNAExtract implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Column(nullable=false)
    private String name;
    @Basic
    protected String method;
    @Basic
    protected String protocol;
    @Basic
    protected String fivePrimer;
    @Basic
    protected String threePrimer;
    @Basic
    protected String targetGene;
    @Basic
    protected String targetFragment;
    @Basic
    @Column(name = "description")
    protected String description;
    //
    @OneToMany(mappedBy = "dnaextract", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    protected Collection<SeqRun> seqruns;
    //
    @ManyToOne
    @JoinColumn(name = "sample_id", nullable = false)
    protected Sample sample;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Collection<SeqRun> getSeqruns() {
        return seqruns;
    }

    public void setSeqruns(Collection<SeqRun> seqruns) {
        this.seqruns = seqruns;
    }

    public void addSeqRun(SeqRun s) {
        getSeqruns().add(s);
        s.setExtract(this);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DNAExtract)) {
            return false;
        }
        DNAExtract other = (DNAExtract) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.DNAExtract[id=" + id + "]";
    }
}
