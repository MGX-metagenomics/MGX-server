/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.upload;

import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.qc.Analyzer;
import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqWriterI;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class SeqFlusher<T extends DNASequenceI> implements Runnable {

    private volatile boolean mayTerminate = false;
    private final long seqrunId;
    private final BlockingQueue<T> in;
    private final GPMSManagedDataSourceI dataSource;
    private final SeqWriterI<T> writer;
    private final Analyzer<T>[] analyzers;
    private volatile Exception error = null;
    private final boolean isPaired;
    //
    //
    // there's a built-in limitation in the number of bind parameters
    // in the postgresql jdbc driver/wire protocol at 32767 which must
    // not be exceeded; with three bound parameters per read, limit
    // this to 10k
    //
    private final static int bulkSize = 10_000;
    //
    private final List<T> holder = new ArrayList<>();
    //
    private final CountDownLatch allDone = new CountDownLatch(1);
    //

    public SeqFlusher(long seqrunId, boolean isPaired, BlockingQueue<T> in, GPMSManagedDataSourceI dataSource, SeqWriterI<T> writer, Analyzer<T>[] analyzers) {
        this.seqrunId = seqrunId;
        this.in = in;
        this.dataSource = dataSource;
        this.writer = writer;
        this.analyzers = analyzers;
        this.isPaired = isPaired;
        dataSource.subscribe(this);
    }

    @Override
    public void run() {

        try {

            if (isPaired) {
                T seq1 = null;
                T seq2 = null;
                while (!(mayTerminate && in.isEmpty())) {

                    while (seq1 == null || seq2 == null) {
                        try {
                            if (seq1 == null) {
                                seq1 = in.poll(500, TimeUnit.MILLISECONDS);
                            }
                            if (seq2 == null) {
                                seq2 = in.poll(500, TimeUnit.MILLISECONDS);
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    processPair(seq1, seq2);
                    seq1 = null;
                    seq2 = null;
                }
            } else {
                T seq = null;
                while (!(mayTerminate && in.isEmpty())) {
                    try {
                        seq = in.poll(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (seq != null) {
                        processSingle(seq);
                    }
                }
            }

            // flush remainder
            if (!holder.isEmpty()) {
                flushChunk();
            }

            writer.close();
        } catch (SequenceException | IOException ex) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            error = ex;
        }

        if (!holder.isEmpty()) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, "Holder not empty when expected? BUG");
            flushChunk();
        }

        assert holder.isEmpty();
        allDone.countDown();
    }

    private void processSingle(T seq) throws SequenceException {
        for (Analyzer<T> a : analyzers) {
            a.add(seq);
        }
        synchronized (holder) {
            holder.add(seq);
        }
        if (holder.size() >= bulkSize) {
            flushChunk();
        }
    }

    private void processPair(T seq1, T seq2) throws SequenceException {
        for (Analyzer<T> a : analyzers) {
            a.addPair(seq1, seq2);
        }
        synchronized (holder) {
            holder.add(seq1);
            holder.add(seq2);
        }
        if (holder.size() >= bulkSize) {
            flushChunk();
        }
    }

    private List<T> fetchChunk() {
        List<T> subList;
        synchronized (holder) {
            int chunk = holder.size() < bulkSize ? holder.size() : bulkSize;
            List<T> sub = holder.subList(0, chunk);
            subList = new ArrayList<>(sub);
            sub.clear(); // since sub is backed by holder, this removes all sub-list items from holder
        }
        return subList;
    }

    protected synchronized void flushChunk() {

        final List<T> commitList = fetchChunk();

        if (commitList.isEmpty()) {
            return;
        }

        String sql = createSQLBulkStatement(commitList.size());
        // insert sequence names and fetch list of generated ids

        int curPos = 0;
        long[] generatedIDs = new long[commitList.size()];

        try ( Connection conn = dataSource.getConnection(this)) {
            try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
                int i = 1;
                for (DNASequenceI s : commitList) {
                    stmt.setLong(i, seqrunId);
                    stmt.setString(i + 1, new String(s.getName()));
                    stmt.setInt(i + 2, s.getSequence().length);
                    i += 3;
                }

                try ( ResultSet res = stmt.executeQuery()) {
                    while (res.next()) {
                        generatedIDs[curPos++] = res.getLong(1);
                    }
                }
            }
        } catch (SequenceException | SQLException ex) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, "Failed statement was: {0}", sql);
            error = ex;
            return;
        }

        //
        // write sequences to persistent storage using the generated ids
        //
        curPos = 0;
        try {
            for (T seq : commitList) {
                // add the generated IDs
                seq.setId(generatedIDs[curPos++]);
                writer.addSequence(seq);
            }
        } catch (SequenceException ex) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            error = ex;
        }
    }

    private static String createSQLBulkStatement(int elements) {
        // build sql bulk insert statement
        StringBuilder sql = new StringBuilder("INSERT INTO read (seqrun_id, name, length) VALUES ");
        for (int cnt = 1; cnt <= elements; cnt++) {
            sql.append("(?,?,?),");
        }
        sql.deleteCharAt(sql.length() - 1); // remove trailing ","

        /*
         * stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
         * ResultSet res = stmt.getGeneratedKeys();
         *
         * leads to 'RETURNING *' internally,
         *
         * therefore we restrict to id column to minimize amount of data
         * that needs to be transferred
         */
        sql.append(" RETURNING id");
        return sql.toString();
    }

    public void complete() throws Exception {
        mayTerminate = true;
        try {
            allDone.await();
        } catch (InterruptedException ex) {
            Throwable inner = ex;
            while (inner.getCause() != null) {
                inner = inner.getCause();
                Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, inner.getMessage());
            }
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (error()) {
            throw getError();
        }
        dataSource.close(this);
    }

    public boolean error() {
        return error != null;
    }

    public Exception getError() {
        Exception e = error;
        error = null;
        return e;
    }

}
