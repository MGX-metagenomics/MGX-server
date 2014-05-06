package de.cebitec.mgx.util;

import de.cebitec.mgx.model.misc.MappedSequence;
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

    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public MappedSequence next() {
        SAMRecord record = this.iterator.next();
        return new MappedSequence(Long.parseLong(record.getReadName()), record.getAlignmentStart(), record.getAlignmentEnd(), getIdentity(record.getCigar()));
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
    }
}
