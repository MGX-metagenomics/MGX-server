package de.cebitec.mgx.upload;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 *
 * @author sjaenick
 */
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FileUploadReceiver implements UploadReceiverI<byte[]> {

    protected FileOutputStream writer = null;
    protected final File targetFile;
    protected final String projectName;
    protected long lastAccessed;

    public FileUploadReceiver(File target, String projectName) throws MGXException {
        this.targetFile = target;
        this.projectName = projectName;
        lastAccessed = System.currentTimeMillis();
        try {
            if (!target.createNewFile()) {
                throw new MGXException("Could not create file");
            }
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

    @Override
    public void cancel() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ex) {
            } finally {
                writer = null;
            }
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }

        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void close() throws MGXException {
        try {
            if (writer != null) {
                writer.close();
            }
            UnixHelper.makeFileGroupWritable(targetFile.getAbsolutePath());
        } catch (IOException ex) {
            throw new MGXException(ex);
        } finally {
            writer = null;
        }
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public void add(byte[] data) throws MGXException {
        if (writer == null) {
            try {
                writer = new FileOutputStream(targetFile);
            } catch (FileNotFoundException ex) {
                throw new MGXException(ex.getMessage());
            }
        }

        try {
            writer.write(data);
        } catch (IOException ex) {
            cancel();
            throw new MGXException(ex);
        }
        lastAccessed = System.currentTimeMillis();
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
