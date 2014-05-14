package de.cebitec.mgx.sessions;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.AutoCloseableSAMRecordIterator;
import de.cebitec.mgx.model.misc.MappedSequence;
import java.io.File;
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
    
    public MappingDataSession(long refId, String projName, String samFile){
        this.refId = refId;
        this.projName = projName;
        lastAccessed = System.currentTimeMillis();
        samReader = new SAMFileReader(new File(samFile));
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) throws MGXException {
        lastAccessed = System.currentTimeMillis();
        SAMRecordIterator overlaps = samReader.queryOverlapping(String.valueOf(refId), from, to);
        return new AutoCloseableSAMRecordIterator(overlaps);
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
