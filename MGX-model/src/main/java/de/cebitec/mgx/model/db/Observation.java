package de.cebitec.mgx.model.db;

/**
 *
 * @author sjaenick
 */
public class Observation {

    private long seq_id;
    //
    private long attr_id;
    //
    protected int start;
    protected int stop;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    public long getSeqId() {
        return seq_id;
    }

    public void setSeq(long seq) {
        this.seq_id = seq;
    }

    public long getAttributeId() {
        return attr_id;
    }

    public void setAttributeId(long attribute) {
        this.attr_id = attribute;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (int) (this.seq_id ^ (this.seq_id >>> 32));
        hash = 67 * hash + (int) (this.attr_id ^ (this.attr_id >>> 32));
        hash = 67 * hash + this.start;
        hash = 67 * hash + this.stop;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Observation other = (Observation) obj;
        if (this.seq_id != other.seq_id) {
            return false;
        }
        if (this.attr_id != other.attr_id) {
            return false;
        }
        if (this.start != other.start) {
            return false;
        }
        if (this.stop != other.stop) {
            return false;
        }
        return true;
    }
    
    

}
