package de.cebitec.mgx.sequence;

import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public interface SeqWriterI<T extends DNASequenceI> {
    
    void addSequence(T seq) throws IOException;

    void close() throws IOException;
}
