package de.cebitec.mgx.model.db;

import java.math.BigDecimal;
import java.util.Collection;

/**
 *
 * @author sjaenick
 */
public class Habitat extends Identifiable {

    private static final long serialVersionUID = 1L;
    protected String name;
    /* GPS location of habitat */
    protected BigDecimal latitude;
    protected BigDecimal longitude;
    protected String description;
    protected int altitude;
    protected String biome;
    protected Collection<Sample> samples;

    public Collection<Sample> getSamples() {
        return samples;
    }

    public Habitat addSample(Sample s) {
        getSamples().add(s);
        s.setHabitatId(getId());
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

    public Double getLatitude() {
        return latitude.doubleValue();
    }

    public Habitat setLatitude(Double latitude) {
        this.latitude = BigDecimal.valueOf(latitude);
        return this;
    }

    public Double getLongitude() {
        return longitude.doubleValue();
    }

    public Habitat setLongitude(Double longitude) {
        this.longitude = BigDecimal.valueOf(longitude);
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
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Habitat)) {
            return false;
        }
        Habitat other = (Habitat) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }
}
