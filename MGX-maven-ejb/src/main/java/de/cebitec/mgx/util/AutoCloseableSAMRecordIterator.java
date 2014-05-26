package de.cebitec.mgx.util;

import de.cebitec.mgx.model.misc.MappedSequence;
import java.util.concurrent.locks.Lock;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author belmann
 */
public class AutoCloseableSAMRecordIterator implements AutoCloseableIterator<MappedSequence> {

    private final SAMRecordIterator iterator;
    private final Lock lock;

    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator, Lock l) {
        this.iterator = iterator;
        this.lock = l;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public MappedSequence next() {
        SAMRecord record = iterator.next();
        // convert to 0-based positions

        if (record.getReadNegativeStrandFlag()) {
         return new MappedSequence(Long.parseLong(record.getReadName()),
                    record.getAlignmentEnd() - 1,
                    record.getAlignmentStart() - 1,
                    getIdentity(record.getCigar()));
        } else {
            return new MappedSequence(Long.parseLong(record.getReadName()),
                    record.getAlignmentStart() - 1,
                    record.getAlignmentEnd() - 1,
                    getIdentity(record.getCigar()));
        }
    }

    private int getIdentity(Cigar c) {
        int matched = 0;
        int alnLen = 0;
        for (CigarElement elem : c.getCigarElements()) {
            if (elem.getOperator() == CigarOperator.HARD_CLIP) {
                continue;
            }
            alnLen += elem.getLength();
            if (elem.getOperator() == CigarOperator.M) {
                matched += elem.getLength();
            }
        }
        return (int) (matched * 100.0d / (alnLen));
    }

    @Override
    public void remove() {
    }

    @Override
    public void close() throws Exception {
        iterator.close();
        lock.unlock();
    }
}
