package de.cebitec.mgx.util;

import java.util.Iterator;

/**
 *
 * @author sj
 */
public interface AutoCloseableIterator<T> extends Iterator<T>, AutoCloseable {
    
}
