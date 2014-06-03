package de.cebitec.mgx.sessions;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.misc.MappedSequence;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.AutoCloseableSAMRecordIterator;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final long refId;
    private final int refLength;
    private final String projName;
    private long lastAccessed;
    private final String samFile;
    private final SAMFileReader samReader;
    private final Lock lock;
    private long maxCov = -1;

    public MappingDataSession(long refId, int refLen, String projName, String samFile) {
        this.refId = refId;
        this.refLength = refLen;
        this.projName = projName;
        this.samFile = samFile;
        lastAccessed = System.currentTimeMillis();
        samReader = new SAMFileReader(new File(samFile));
        samReader.setValidationStringency(SAMFileReader.ValidationStringency.STRICT);
        lock = new ReentrantLock();
    }

    public AutoCloseableIterator<MappedSequence> get(int from, int to) throws MGXException {
        if (from > to) {
            throw new IllegalArgumentException();
        }
        lastAccessed = System.currentTimeMillis();
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                // query 1-based
                SAMRecordIterator overlaps = samReader.queryOverlapping(String.valueOf(refId), from + 1, to + 1);
                return new AutoCloseableSAMRecordIterator(overlaps, lock);
            } else {
                throw new MGXException("Failed to acquire lock, please try again later.");
            }
        } catch (InterruptedException ex) {
            throw new MGXException(ex);
        }
    }

    public long getMaxCoverage() {
        if (maxCov == -1) {
            ForkJoinPool pool = new ForkJoinPool();
            // sam-jdk wants 1-based positions - ARGH
            GetCoverage getCov = new GetCoverage(samFile, 1, refLength, refId);
            maxCov = pool.invoke(getCov);
            pool.shutdown();
        }
        lastAccessed = System.currentTimeMillis();
        return maxCov;
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

    public static class GetCoverage extends RecursiveTask<Long> {

        private final String samFile;
        private final int from;  // 1-based
        private final int to; // 1-based
        private final long refId;

        private final static int THRESHOLD = 5000;

        /*
         * 1-based positions !!!
         */
        public GetCoverage(String samFile, int from, int to, long refId) {
            this.samFile = samFile;
            this.from = from;
            this.to = to;
            this.refId = refId;
        }

        @Override
        protected Long compute() {
            int length = from - to + 1;

            if (length > THRESHOLD) {
                // split into two tasks
                int mid = length / 2;
                GetCoverage left = new GetCoverage(samFile, from, mid, refId);
                left.fork();
                GetCoverage right = new GetCoverage(samFile, mid + 1, to, refId);
                //right.compute();

                long maxLeft = left.join();
                long maxRight = right.compute();
                return Math.max(maxLeft, maxRight);
            }

            int[] coverage = new int[to - from + 1];
            Arrays.fill(coverage, 0);
            SAMFileReader samReader = new SAMFileReader(new File(samFile));
            SAMRecordIterator iter = samReader.queryOverlapping(String.valueOf(refId), from, to);
            while (iter.hasNext()) {
                SAMRecord record = iter.next();
                assert record.getAlignmentStart() < record.getAlignmentEnd();

                for (int i = record.getAlignmentStart(); i <= record.getAlignmentEnd(); i++) {
                    // we need to check extra since we also receive mappings
                    // which only partially overlap with the interval
                    if (i >= from && i <= to) {
                        coverage[i - from]++;
                    }
                }
            }
            iter.close();
            samReader.close();
            long max = 0;
            for (int i : coverage) {
                if (i > max) {
                    max = i;
                }
            }
            return max;
        }

    }
}
