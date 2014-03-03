
package de.cebitec.mgx.util;

import de.cebitec.mgx.model.misc.MappedSequence;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author belmann
 */
public class AutoCloseableSAMRecordIterator implements AutoCloseableIterator<MappedSequence> {

    private final SAMRecordIterator iterator;
    int counter;

    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator) {
        this.iterator = iterator;
        counter = -1;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public MappedSequence next() {
        SAMRecord record = this.iterator.next();
        counter++;
        return new MappedSequence(counter, record.getAlignmentStart(), record.getAlignmentEnd(), getIdentity(record));
    }

    private int getIdentity(SAMRecord rec) {
        int matched = 0;
        for (CigarElement elem : rec.getCigar().getCigarElements()) {
            if (elem.getOperator().name().equals("M")) {
                matched += elem.getLength();
            }
        }
        int alignment =  (int) (matched * 100.0d/ ((rec.getAlignmentEnd() - rec.getAlignmentStart()) + 1));
        return alignment;
    }

    @Override
    public void remove() {
    }

    @Override
    public void close() throws Exception {
        iterator.close();
    }
}
