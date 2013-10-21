package de.cebitec.mgx.util;

import de.cebitec.mgx.model.db.Observation;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sj
 */
public class SearchResult {

    private String sequence_name;
    private int sequence_length;
    private final List<Observation> observations = new ArrayList<>();

    public int getSequenceLength() {
        return sequence_length;
    }

    public void setSequenceLength(int sequence_length) {
        this.sequence_length = sequence_length;
    }

    public String getSequenceName() {
        return sequence_name;
    }

    public void setSequenceName(String sequence_name) {
        this.sequence_name = sequence_name;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public void addObservation(Observation obs) {
        observations.add(obs);
    }
}
