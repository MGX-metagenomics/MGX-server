package de.cebitec.mgx.util;

/**
 *
 * @author sjaenick
 */
public class LimitingIterator<T> implements AutoCloseableIterator<T> {

    private final int limit;
    private int count = 1;
    private final AutoCloseableIterator<T> iter;
    private T next = null;
    private long lastUsed;

    public LimitingIterator(int limit, AutoCloseableIterator<T> iter) {
        this.limit = limit;
        this.iter = iter;
        lastUsed = System.currentTimeMillis();
    }

    @Override
    public boolean hasNext() {
        lastUsed = System.currentTimeMillis();
        if (next != null) {
            return true;
        }
        if (count <= limit && iter.hasNext()) {
            next = iter.next();
            count++;
            return true;
        }
        return false;
    }

    @Override
    public T next() {
        lastUsed = System.currentTimeMillis();
        T ret = next;
        next = null;
        return ret;
    }

    public boolean limitReached() {
        lastUsed = System.currentTimeMillis();
        return count >= limit;
    }

    public void advanceOverLimit() {
        lastUsed = System.currentTimeMillis();
        count = 1;
    }

    @Override
    public void close() {
        if (!limitReached()) {
            iter.close();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

    public long lastAccessed() {
        return lastUsed;
    }
}
