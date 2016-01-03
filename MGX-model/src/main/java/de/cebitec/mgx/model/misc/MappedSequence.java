package de.cebitec.mgx.model.misc;

/**
 *
 * @author sj
 */

public class MappedSequence {
    
    private final long seq_id;
    private final int[] coords = new int[2];
    private final float identity;

    public MappedSequence(long seq_id, int start, int stop, float identity) {
        this.seq_id = seq_id;
        this.coords[0] = start;
        this.coords[1] = stop;
        this.identity = identity;
    }

    public long getSeqId() {
        return seq_id;
    }

    public int getStart() {
        return coords[0];
    }

    public int getStop() {
        return coords[1];
    }

    public float getIdentity() {
        return identity;
    }
}