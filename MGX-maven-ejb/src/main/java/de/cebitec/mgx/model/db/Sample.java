/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Sample")
public class Sample implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "collectiondate")
    protected Date collectiondate;
    @Basic
    @NotNull
    @Column(name = "material")
    protected String material;
    @Basic
    @Column(name = "temperature", precision=11, scale=8)
    protected BigDecimal temperature;
    @Basic
    @Column(name = "volume")
    protected int volume;
    @Basic
    @Column(name = "volume_unit")
    protected String volume_unit;
    //
    @OneToMany(mappedBy = "sample", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    protected Collection<DNAExtract> extracts;
    //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habitat_id", nullable = false)
    protected Habitat habitat;

    public Habitat getHabitat() {
        return habitat;
    }

    public Sample setHabitat(Habitat h) {
        habitat = h;
        return this;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Sample setId(Long id) {
        this.id = id;
        return this;
    }

    public Date getCollectionDate() {
        return collectiondate;
    }

    public Sample setCollectionDate(Date collectiondate) {
        this.collectiondate = collectiondate;
        return this;
    }

    public String getMaterial() {
        return material;
    }

    public Sample setMaterial(String material) {
        this.material = material;
        return this;
    }

    public double getTemperature() {
        return temperature.doubleValue();
    }

    public Sample setTemperature(double temperature) {
        this.temperature = BigDecimal.valueOf(temperature);
        return this;
    }

    public int getVolume() {
        return volume;
    }

    public Sample setVolume(int volume) {
        this.volume = volume;
        return this;
    }

    public String getVolumeUnit() {
        return volume_unit;
    }

    public Sample setVolumeUnit(String volume_unit) {
        this.volume_unit = volume_unit;
        return this;
    }

    public Collection<DNAExtract> getDNAExtracts() {
        return extracts;
    }

    public Sample setDNAExtracts(Collection<DNAExtract> extracts) {
        this.extracts = extracts;
        return this;
    }

    public Sample addDNAExtract(DNAExtract d) {
        getDNAExtracts().add(d);
        d.setSample(this);
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
        if (!(object instanceof Sample)) {
            return false;
        }
        Sample other = (Sample) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Sample[id=" + id + "]";
    }
}
