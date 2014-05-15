package de.cebitec.mgx.sessions;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.AutoCloseableSAMRecordIterator;
import de.cebitec.mgx.model.misc.MappedSequence;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final long refId;
    private final String projName;
    private long lastAccessed;
    private final SAMFileReader samReader;
    private final Lock lock;

    public MappingDataSession(long refId, String projName, String samFile) {
        this.refId = refId;
        this.projName = projName;
        lastAccessed = System.currentTimeMillis();
        samReader = new SAMFileReader(new File(samFile));
        lock = new ReentrantLock();
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) throws MGXException {
        lastAccessed = System.currentTimeMillis();
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                SAMRecordIterator overlaps = samReader.queryOverlapping(String.valueOf(refId), from, to);
                return new AutoCloseableSAMRecordIterator(overlaps, lock);
            } else {
                throw new MGXException("Failed to acquire lock, please try again later.");
            }
        } catch (InterruptedException ex) {
            throw new MGXException(ex);
        }
        return null;
    }

    public long lastAccessed() {
        return lastAccessed;
    }

    public String getProjectName() {
        return projName;
    }

    public void close() {
        samReader.close();
    }

}
