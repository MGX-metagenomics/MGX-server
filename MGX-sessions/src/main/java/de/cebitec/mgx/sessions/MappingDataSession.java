package de.cebitec.mgx.sessions;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.misc.MappedSequence;
import de.cebitec.mgx.sessions.util.AutoCloseableSAMRecordIterator;
import de.cebitec.mgx.util.AutoCloseableIterator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class MappingDataSession {

    private final long mappingId;
    private final long refId;
    private final int refLength;
    private final String projName;
    private long lastAccessed;
    private final File samFile;
    private final SamReader samReader;
    private final Lock lock;
    private long maxCov = -1;
    private long referenceCov = -1;

    public MappingDataSession(long mappingId, long refId, int refLen, String projName, File samFile) {
        this.mappingId = mappingId;
        this.refId = refId;
        this.refLength = refLen;
        this.projName = projName;
        this.samFile = samFile;
        lastAccessed = System.currentTimeMillis();
        samReader = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.STRICT).open(samFile);
        lock = new ReentrantLock();
    }

    public long getMappingId() {
        return mappingId;
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
            Logger.getLogger(MappingDataSession.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
    }

    public long getMaxCoverage() throws MGXException {
        if (maxCov == -1L) {
            File covFile = new File(samFile + ".maxCov");
            if (covFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(covFile))) {
                    String line = br.readLine();
                    maxCov = Long.parseLong(line);
                } catch (IOException | NumberFormatException ex) {
                    // ignore
                }
            }

            if (maxCov != -1L) {
                return maxCov;
            }

            ForkJoinPool pool = new ForkJoinPool();
            // sam-jdk wants 1-based positions - ARGH
            GetCoverage getCov = new GetCoverage(samFile, 1, refLength, String.valueOf(refId));
            maxCov = pool.invoke(getCov);
            pool.shutdown();

            if (maxCov == -1L) {
                throw new MGXException("Unable to compute max. coverage, internal error.");
            }

            // save result
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(samFile + ".maxCov"))) {
                bw.write(String.valueOf(maxCov));
            } catch (IOException ex) {
                // ignore
            }
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
        try {
            samReader.close();
        } catch (IOException ex) {
            Logger.getLogger(MappingDataSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getGenomicCoverage() throws MGXException {
        if (referenceCov == -1L) {
            File covFile = new File(samFile + ".refCov");
            if (covFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(covFile))) {
                    String line = br.readLine();
                    referenceCov = Long.parseLong(line);
                } catch (IOException | NumberFormatException ex) {
                    // ignore
                }
            }

            if (referenceCov != -1L) {
                return referenceCov;
            }

            ForkJoinPool pool = new ForkJoinPool();
            // sam-jdk wants 1-based positions - ARGH
            GetReferenceCoverage getCov = new GetReferenceCoverage(samFile, 1, refLength, String.valueOf(refId));
            referenceCov = pool.invoke(getCov);
            pool.shutdown();

            if (referenceCov == -1L) {
                throw new MGXException("Unable to compute reference coverage, internal error.");
            }

            // save result
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(samFile + ".refCov"))) {
                bw.write(String.valueOf(referenceCov));
            } catch (IOException ex) {
                // ignore
            }
        }
        lastAccessed = System.currentTimeMillis();

        return referenceCov;
    }

    public static class GetReferenceCoverage extends RecursiveTask<Long> {

        private final File samFile;
        private final int from;  // 1-based
        private final int to; // 1-based
        private final String refId;

        private final static int THRESHOLD = 50_000;

        /*
         * 1-based positions !!!
         */
        public GetReferenceCoverage(File samFile, int from, int to, String refId) {
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
                GetReferenceCoverage left = new GetReferenceCoverage(samFile, from, mid, refId);
                left.fork();
                GetReferenceCoverage right = new GetReferenceCoverage(samFile, mid + 1, to, refId);
                //right.compute();

                long covLeft = left.join();
                long covRight = right.compute();
                if (covLeft == -1L || covRight == -1L) {
                    return -1L; // failure in subtask
                }
                return covLeft + covRight;
            }

            BitSet coverage = new BitSet(to - from + 1);
            SamReader samReader = SamReaderFactory.makeDefault().open(samFile);
            SAMRecordIterator iter = samReader.queryOverlapping(refId, from, to);

            while (iter.hasNext()) {
                SAMRecord record = iter.next();
                for (int i = record.getAlignmentStart(); i <= record.getAlignmentEnd(); i++) {
                    // we need to check extra since we also receive mappings
                    // which only partially overlap with the interval
                    if (i >= from && i <= to) {
                        coverage.set(i - from);
                    }
                }
            }
            iter.close();

            try {
                samReader.close();
            } catch (IOException ex) {
                Logger.getLogger(MappingDataSession.class.getName()).log(Level.SEVERE, null, ex);
            }

            return (long) coverage.cardinality();
        }

    }

    public static class GetCoverage extends RecursiveTask<Long> {

        private final File samFile;
        private final int from;  // 1-based
        private final int to; // 1-based
        private final String refId;

        private final static int THRESHOLD = 5000;

        /*
         * 1-based positions !!!
         */
        public GetCoverage(File samFile, int from, int to, String refId) {
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
                if (maxLeft == -1L || maxRight == -1L) {
                    return -1L; // failure in subtask
                }
                return Math.max(maxLeft, maxRight);
            }

            int[] coverage = new int[to - from + 1];
            Arrays.fill(coverage, 0);
            SamReader samReader = SamReaderFactory.makeDefault().open(samFile);
            //SAMFileReader samReader = new SAMFileReader(new File(samFile));
            SAMRecordIterator iter = samReader.queryOverlapping(refId, from, to);

            while (iter.hasNext()) {
                SAMRecord record = iter.next();
                for (int i = record.getAlignmentStart(); i <= record.getAlignmentEnd(); i++) {
                    // we need to check extra since we also receive mappings
                    // which only partially overlap with the interval
                    if (i >= from && i <= to) {
                        coverage[i - from]++;
                    }
                }
            }
            iter.close();

            try {
                samReader.close();
            } catch (IOException ex) {
                Logger.getLogger(MappingDataSession.class.getName()).log(Level.SEVERE, null, ex);
            }

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
