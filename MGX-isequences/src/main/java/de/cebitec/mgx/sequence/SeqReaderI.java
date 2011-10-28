
package de.cebitec.mgx.sequence;

import java.util.Enumeration;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public interface SeqReaderI extends Enumeration<DNASequenceI> {
    
    public void close();
    public void delete();
    public Set<DNASequenceI> fetch(Set<Long> ids) throws SeqStoreException;
    
}
