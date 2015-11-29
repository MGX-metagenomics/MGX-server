package de.cebitec.mgx.download;

import de.cebitec.mgx.core.MGXException;

/**
 *
 * @author sjaenick
 * @param <T>
 */
public interface DownloadProviderI<T> {

    public void cancel();

    public void close() throws MGXException;

    public T fetch() throws MGXException;
    
    public boolean isFinished();

    public String getProjectName();

    public long lastAccessed();
}
