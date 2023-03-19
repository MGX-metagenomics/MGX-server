package de.cebitec.mgx.model.db;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author sjaenick
 */
public class Sample extends Identifiable {

    protected Date collectiondate = null;
    protected String material;
    protected BigDecimal temperature;
    protected int volume;
    protected String volume_unit;
    //
    protected Collection<DNAExtract> extracts;
    //
    protected long habitat;

    public long getHabitatId() {
        return habitat;
    }

    public Sample setHabitatId(long h) {
        habitat = h;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Sample)) {
            return false;
        }
        Sample other = (Sample) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER)
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }
}
