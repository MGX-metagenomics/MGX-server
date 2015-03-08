package de.cebitec.mgx.util;

import de.cebitec.mgx.model.misc.MappedSequence;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author belmann
 */
public class AutoCloseableSAMRecordIterator implements AutoCloseableIterator<MappedSequence> {

    private final SAMRecordIterator iterator;
    private final Connection conn;
    private final Lock lock;
    private MappedSequence curElem = null;

    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator, Connection conn, Lock l) {
        this.iterator = iterator;
        this.conn = conn;
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

            // sequence might have been marked discard after the mapping was created,
            // need to check and filter
            boolean discard = false;
            long seqId = Long.parseLong(record.getReadName());

            try (PreparedStatement stmt = conn.prepareStatement("SELECT discard FROM read WHERE id=?")) {
                stmt.setLong(1, seqId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        discard = rs.getBoolean(1);
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, null, ex);
            }

            if (!discard) {
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
        conn.close();
        lock.unlock();
    }
}
