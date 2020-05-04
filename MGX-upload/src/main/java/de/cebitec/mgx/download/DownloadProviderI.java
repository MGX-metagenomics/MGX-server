package de.cebitec.mgx.download;

import de.cebitec.mgx.core.MGXException;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public interface DownloadProviderI<T> extends Runnable {

    public void cancel();

    public void close() throws MGXException;

    public T fetch() throws MGXException;
    
    public String getProjectName();

    public long lastAccessed();
}
