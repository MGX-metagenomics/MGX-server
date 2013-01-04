package de.cebitec.mgx.download;

import de.cebitec.mgx.controller.MGXException;

/**
 *
 * @author sjaenick
 */
public interface DownloadProviderI<T> {

    public void cancel();

    public void close() throws MGXException;

    public T fetch() throws MGXException;
    
    public boolean isFinished();

    public String getProjectName();

    public long lastAccessed();
}
