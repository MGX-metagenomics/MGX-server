package de.cebitec.mgx.model.misc;

/**
 *
 * @author sj
 */

public class MappedSequence {
    
    private final long seq_id;
    private final int start;
    private final int stop;
    private final int identity; // range 0-255

    public MappedSequence(long seq_id, int start, int stop, int identity) {
        this.seq_id = seq_id;
        this.start = start;
        this.stop = stop;
        this.identity = identity;
    }

    public long getSeqId() {
        return seq_id;
    }

    public int getStart() {
        return start;
    }

    public int getStop() {
        return stop;
    }

    public int getIdentity() {
        return identity;
    }
}
