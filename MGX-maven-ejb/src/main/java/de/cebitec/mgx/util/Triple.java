package de.cebitec.mgx.util;

/**
 *
 * @author sjaenick
 */
public class Triple<T, U, V> {

    private final T first;
    private final U second;
    private final V third;

    public Triple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }
    
    public V getThird() {
        return third;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Triple<T, U, V> other = (Triple<T, U, V>) obj;
        if (this.first != other.first && (this.first == null || !this.first.equals(other.first))) {
            return false;
        }
        if (this.second != other.second && (this.second == null || !this.second.equals(other.second))) {
            return false;
        }
        if (this.third != other.third && (this.third == null || !this.third.equals(other.third))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 37 * hash + (this.second != null ? this.second.hashCode() : 0);
        hash = 37 * hash + (this.third != null ? this.third.hashCode() : 0);
        return hash;
    }
    
    
}
