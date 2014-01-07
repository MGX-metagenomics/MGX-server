package de.cebitec.mgx.util;

import java.util.Iterator;

/**
 *
 * @author sj
 * @param <T>
 */
public interface AutoCloseableIterator<T> extends Iterator<T>, AutoCloseable {
    
}
