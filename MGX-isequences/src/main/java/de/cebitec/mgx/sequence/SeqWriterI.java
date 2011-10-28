package de.cebitec.mgx.sequence;

import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public interface SeqWriterI {
    
    void addSequence(DNASequenceI seq) throws IOException;

    void close() throws IOException;
}
