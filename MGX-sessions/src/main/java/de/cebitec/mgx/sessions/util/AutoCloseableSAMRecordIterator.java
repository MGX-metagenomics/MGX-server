package de.cebitec.mgx.sessions.util;

import de.cebitec.mgx.model.misc.MappedSequence;
import de.cebitec.mgx.util.AutoCloseableIterator;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author belmann
 */
public class AutoCloseableSAMRecordIterator implements AutoCloseableIterator<MappedSequence> {

    private final SAMRecordIterator iterator;
    private final Lock lock;
    private MappedSequence curElem = null;

    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator, Lock l) {
        this.iterator = iterator;
        this.lock = l;
    }

    @Override
    public boolean hasNext() {
        fetch();
        return curElem != null;
    }

    @Override
    public MappedSequence next() {
        if (curElem == null) {
            throw new NoSuchElementException();
        }
        MappedSequence ret = curElem;
        curElem = null;
        return ret;
    }

    private void fetch() {
        while (curElem == null && iterator.hasNext()) {

            SAMRecord record = iterator.next();

            // convert to 0-based positions
            if (record.getReadNegativeStrandFlag()) {
                curElem = new MappedSequence(Long.parseLong(record.getReadName()),
                        record.getAlignmentEnd() - 1,
                        record.getAlignmentStart() - 1,
                        getIdentity(record.getCigar()));
            } else {
                curElem = new MappedSequence(Long.parseLong(record.getReadName()),
                        record.getAlignmentStart() - 1,
                        record.getAlignmentEnd() - 1,
                        getIdentity(record.getCigar()));
            }
        }
    }

    private static float getIdentity(Cigar c) {
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
        return (float) (matched * 100.0d / (alnLen));
    }

    @Override
    public void remove() {
    }

    @Override
    public void close() {
        iterator.close();
        lock.unlock();
    }
}
