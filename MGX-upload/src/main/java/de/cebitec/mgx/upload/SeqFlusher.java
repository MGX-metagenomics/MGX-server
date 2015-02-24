/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.upload;

import de.cebitec.mgx.qc.Analyzer;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqWriterI;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
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
public class SeqFlusher implements Runnable {

    private volatile boolean mayTerminate = false;
    private final long seqrunId;
    private final BlockingQueue<DNASequenceI> in;
    private final Connection conn;
    private final SeqWriterI writer;
    private final Analyzer[] analyzers;
    private Exception error = null;
    private final int bulkSize;
    //
    private final List<DNASequenceI> holder = new ArrayList<>();
    //
    private final CountDownLatch allDone = new CountDownLatch(1);
    //
    private int waitMs = 5;

    public SeqFlusher(long seqrunId, BlockingQueue<DNASequenceI> in, Connection conn, SeqWriterI writer, Analyzer[] analyzers, int bulkSize) {
        this.seqrunId = seqrunId;
        this.in = in;
        this.conn = conn;
        this.writer = writer;
        this.analyzers = analyzers;
        this.bulkSize = bulkSize;
    }

    @Override
    public void run() {
        DNASequenceI seq = null;
        while (!mayTerminate) {
            try {
                seq = in.poll(waitMs, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (seq != null) {
                waitMs-- ; waitMs = waitMs < 1 ? 1 : waitMs;
                process(seq);
            }
        }

        // flush
        while (!in.isEmpty()) {
            seq = in.poll();
            if (seq != null) {
                process(seq);
            }
        }

        // flush remainder
        flushChunk();

        assert holder.isEmpty();

        try {
            writer.close();
        } catch (Exception ex) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            error = ex;
        }

        allDone.countDown();
    }

    private void process(DNASequenceI seq) {
        holder.add(seq);
        for (Analyzer a : analyzers) {
            a.add(seq);
        }
        if (holder.size() >= bulkSize) {
            flushChunk();
        }
    }

    private synchronized List<? extends DNASequenceI> fetchChunk() {
        int chunk = holder.size() < bulkSize ? holder.size() : bulkSize;
        List<? extends DNASequenceI> sub = holder.subList(0, chunk);
        List<? extends DNASequenceI> subList = new ArrayList<>(sub);
        sub.clear(); // since sub is backed by holder, this removes all sub-list items from holder
        return subList;
    }

    protected synchronized void flushChunk() {

        final List<? extends DNASequenceI> commitList = fetchChunk();

        String sql = createSQLBulkStatement(commitList.size());
        // insert sequence names and fetch list of generated ids

        int curPos = 0;
        long[] generatedIDs = new long[commitList.size()];

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (DNASequenceI s : commitList) {
                stmt.setLong(i, seqrunId);
                stmt.setString(i + 1, new String(s.getName()));
                stmt.setInt(i + 2, s.getSequence().length);
                i += 3;
            }

            try (ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    generatedIDs[curPos++] = res.getLong(1);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SeqFlusher.class.getName()).log(Level.SEVERE, null, ex);
            error = ex;
            return;
        }

        //
        // write sequences to persistent storage using the generated ids
        //
        curPos = 0;
        try {
            for (Iterator<? extends DNASequenceI> iter = commitList.iterator(); iter.hasNext();) {
                // add the generated IDs
                DNASequenceI s = iter.next();
                s.setId(generatedIDs[curPos++]);
                writer.addSequence(s);
            }
        } catch (IOException ex) {
            error = ex;
        }

    }

    private String createSQLBulkStatement(int elements) {
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
        if (error()) {
            throw getError();
        }
        allDone.await();
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
