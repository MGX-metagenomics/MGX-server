
package de.cebitec.mgx.sequence;

import java.util.Enumeration;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public interface SeqReaderI<T extends DNASequenceI> extends Enumeration<T> {
    
    public void close();
    public void delete();
    public Set<T> fetch(Set<Long> ids) throws SeqStoreException;
    
}
