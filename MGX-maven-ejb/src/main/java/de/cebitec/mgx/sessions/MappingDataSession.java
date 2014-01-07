package de.cebitec.mgx.sessions;

import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.AutoCloseableSAMRecordIterator;
import de.cebitec.mgx.util.MappedSequence;
import java.io.File;
import net.sf.samtools.SAMFileReader;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final Reference ref;
    private final String projName;
    private long lastAccessed;
    private SAMFileReader samReader;      
    private String samFile;
    
    
    public MappingDataSession(Reference ref, String projName,String samFile) {
        this.ref = ref;
        this.projName = projName;
        lastAccessed = System.currentTimeMillis();
        this.samFile = samFile;
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) {
        lastAccessed = System.currentTimeMillis(); 
        samReader = new SAMFileReader(new File(this.samFile));
        return new AutoCloseableSAMRecordIterator(samReader.iterator());
    }

    public long lastAccessed() {
        return lastAccessed;
    }

    public String getProjectName() {
        return projName;
    }

    public void close() {

    }

}
