
package de.cebitec.mgx.sequence;

/**
 *
 * @author sjaenick
 */
public interface FactoryI {
    public SeqReaderI getReader(String uri) throws SeqStoreException;
}
