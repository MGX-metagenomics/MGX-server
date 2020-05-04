package de.cebitec.mgx.download;

import de.cebitec.mgx.core.MGXException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author sjaenick
 */
public class FileDownloadProvider implements DownloadProviderI<byte[]> {

    private final static int DEFAULT_CHUNK = 4096;

    private final String projectName;
    private long lastAccessed;
    private FileInputStream fis;
    private final File file;
    private boolean finished = false;
    private final byte[] buf = new byte[DEFAULT_CHUNK];

    public FileDownloadProvider(String projectName, File f) {
        this.projectName = projectName;
        this.file = f;
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void cancel() {
        try {
            close();
        } catch (MGXException ex) {
        }
    }

    @Override
    public void close() throws MGXException {
        if (fis != null) {
            try {
                fis.close();
                fis = null;
            } catch (IOException ex) {
                throw new MGXException(ex);
            }
        }
    }

    @Override
    public byte[] fetch() throws MGXException {
        if (finished) {
            return new byte[]{};
        }
        
        if (fis == null) {
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                throw new MGXException(ex);
            }
        }
        lastAccessed = System.currentTimeMillis();

        try {
            int numBytes = fis.read(buf);
            if (numBytes < DEFAULT_CHUNK) {
                finished = true;
                fis.close();
                fis = null;
            }
            if (numBytes == -1) {
                return new byte[]{};
            }
            return Arrays.copyOf(buf, numBytes);
        } catch (IOException ex) {
            throw new MGXException(ex);
        }
    }

    @Override
    public void run() {
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

}
