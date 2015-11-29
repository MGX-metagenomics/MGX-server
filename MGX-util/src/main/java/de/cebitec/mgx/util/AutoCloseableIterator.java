package de.cebitec.mgx.util;

import java.util.Iterator;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public interface AutoCloseableIterator<T> extends Iterator<T>, AutoCloseable {
    
}
