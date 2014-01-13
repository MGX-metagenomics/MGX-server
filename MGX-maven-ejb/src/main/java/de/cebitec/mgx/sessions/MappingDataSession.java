package de.cebitec.mgx.sessions;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.AutoCloseableSAMRecordIterator;
import de.cebitec.mgx.util.MappedSequence;
import java.io.File;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final Reference ref;
    private final String projName;
    private long lastAccessed;
    private final SAMFileReader samReader;
    
    public MappingDataSession(Reference ref, String projName, String samFile){
        this.ref = ref;
        this.projName = projName;
        lastAccessed = System.currentTimeMillis();
        samReader = new SAMFileReader(new File(samFile));
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) throws MGXException {
        lastAccessed = System.currentTimeMillis();
        SAMRecordIterator overlaps = samReader.queryOverlapping(ref.getId().toString(), from, to);
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
