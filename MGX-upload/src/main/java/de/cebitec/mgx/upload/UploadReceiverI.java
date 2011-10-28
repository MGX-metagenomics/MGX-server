
package de.cebitec.mgx.upload;

import de.cebitec.mgx.controller.MGXException;

/**
 *
 * @author sjaenick
 */
public interface UploadReceiverI<T> {

    public void cancel();
    public void close() throws MGXException;
    public void add(T data) throws MGXException;
    public String getProjectName();
    public long lastAccessed();

}
