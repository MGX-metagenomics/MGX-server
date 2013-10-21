package de.cebitec.mgx.sessions;

import de.cebitec.mgx.model.db.Reference;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.MappedSequence;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final Reference ref;
    private final String projName;
    private long lastAccessed;

    public MappingDataSession(Reference ref, String projName) {
        this.ref = ref;
        this.projName = projName;
        lastAccessed = System.currentTimeMillis();
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) {
        lastAccessed = System.currentTimeMillis();
        return null;
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
