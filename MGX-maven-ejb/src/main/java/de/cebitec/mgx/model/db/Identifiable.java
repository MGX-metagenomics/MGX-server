
package de.cebitec.mgx.model.db;

/**
 *
 * @author sjaenick
 */
public interface Identifiable {
    
    public static long INVALID_IDENTIFIER = -1;
    
    public Long getId();
}
