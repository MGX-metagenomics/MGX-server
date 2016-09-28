package de.cebitec.mgx.util;

import java.util.Iterator;

/**
 *
 * @author sj
 */
public class ForwardingIterator<T> implements AutoCloseableIterator<T> {

    private final Iterator<T> iter;

    public ForwardingIterator(Iterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return iter != null && iter.hasNext();
    }

    @Override
    public T next() {
        return iter.next();
    }

    @Override
    public void remove() {
        iter.remove();
    }

    @Override
    public void close() {
        // no-op
    }
}
