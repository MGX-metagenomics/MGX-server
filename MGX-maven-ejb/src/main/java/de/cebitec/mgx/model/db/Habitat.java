package de.cebitec.mgx.model.db;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Habitat", uniqueConstraints =
@UniqueConstraint(columnNames = {"name"}))
public class Habitat implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    //@SequenceGenerator(name="habitat_seq", sequenceName="habitat_seq")
    //@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="habitat_seq")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Basic
    @NotNull
    @Column(name = "name")
    protected String name;
    /* GPS location of habitat */
    @Basic
    @NotNull
    @Column(name = "location")
    protected String location;
    @Basic
    @Column(name = "description")
    protected String description;
    @Basic
    protected int altitude;
    @Basic
    protected String biome;
    @OneToMany(mappedBy = "habitat", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    protected Collection<Sample> samples;

    @Override
    public Long getId() {
        return id;
    }

    public Habitat setId(Long id) {
        this.id = id;
        return this;
    }

    public Collection<Sample> getSamples() {
        return samples;
    }

    public Habitat addSample(Sample s) {
        getSamples().add(s);
        s.setHabitat(this);
        return this;
    }

    public Habitat setSamples(Collection<Sample> samples) {
        this.samples = samples;
        return this;
    }

    public String getName() {
        return name;
    }

    public Habitat setName(String name) {
        this.name = name;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public Habitat setLocation(String location) {
        this.location = location;
        return this;
    }

    public int getAltitude() {
        return altitude;
    }

    public Habitat setAltitude(int altitude) {
        this.altitude = altitude;
        return this;
    }

    public String getBiome() {
        return biome;
    }

    public Habitat setBiome(String biome) {
        this.biome = biome;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Habitat setDescription(String description) {
        this.description = description;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Habitat)) {
            return false;
        }
        Habitat other = (Habitat) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Habitat[id=" + id + "]";
    }
}
