package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class SequenceDAO extends DAO<Sequence> {

    public SequenceDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<Sequence> getType() {
        return Sequence.class;
    }

    private final static String CREATE = "INSERT INTO read (name, length, seqrun_id, discard) "
            + "VALUES (?,?,?,?) RETURNING id";

    public long create(Sequence obj, long seqrunId) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getName());
                stmt.setInt(2, obj.getLength());
                stmt.setLong(3, seqrunId);
                stmt.setBoolean(4, false);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return obj.getId();
    }

    private static final String GET_SEQRUN = "SELECT r.seqrun_id, r.name FROM read r WHERE r.id=?";

    @Override
    @SuppressWarnings("unchecked")
    public Sequence getById(long readId) throws MGXException {
        if (readId <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        // find the storage file for this sequence
        long seqrun_id = -1;
        String seqName = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(GET_SEQRUN)) {
                stmt.setLong(1, readId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        seqrun_id = rs.getLong(1);
                        seqName = rs.getString(2);
                    } else {
                        throw new MGXException("No object of type Sequence for ID " + readId);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        String dbFile = null;
        try {
            dbFile = getController().getSeqRunDAO().getDBFile(seqrun_id).getAbsolutePath();
        } catch (IOException ex) {
            Logger.getLogger(SequenceDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (dbFile == null || dbFile.isEmpty() || seqName == null || seqName.isEmpty()) {
            throw new MGXException("No sequence for ID " + readId);
        }

        Sequence seq = new Sequence();
        seq.setId(readId);
        seq.setName(seqName);

        // read sequence data
        try (SeqReaderI<? extends DNASequenceI> reader = SeqReaderFactory.<DNASequenceI>getReader(dbFile)) {
            Iterator<? extends DNASequenceI> iter = reader.fetch(new long[]{readId}).iterator();
            if (iter.hasNext()) {
                DNASequenceI dnaSeq = iter.next();
                byte[] seqdata = dnaSeq.getSequence();
                String seqString = new String(seqdata).toUpperCase();
                seq.setSequence(seqString);
                seq.setLength(seqString.length());
            } else {
                throw new MGXException("No sequence data found for ID " + readId);
            }
        } catch (SequenceException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        return seq;
    }

    public AutoCloseableIterator<Sequence> getByIds(Collection<Long> ids) throws MGXException {
        if (ids == null || ids.isEmpty()) {
            throw new MGXException("Null/empty ID list.");
        }
        if (ids.size() > 10_000) {
            throw new MGXException("Chunk too large, please do not request more than 10k sequences.");
        }

        String GET_BY_IDS = "SELECT id, name, length FROM read WHERE id IN (" + toSQLTemplateString(ids.size()) + ")";

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(GET_BY_IDS);
            int idx = 1;
            for (Long id : ids) {
                stmt.setLong(idx++, id);
            }
            ResultSet rs = stmt.executeQuery();

            return new DBIterator<Sequence>(rs, stmt, conn) {
                @Override
                public Sequence convert(ResultSet rs) throws SQLException {
                    Sequence seq = new Sequence();
                    seq.setId(rs.getLong(1));
                    seq.setName(rs.getString(2));
                    seq.setLength(rs.getInt(3));
                    return seq;
                }
            };
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

    }

    public Sequence byName(long runId, String seqName) throws MGXException {
        try ( Connection conn = getConnection()) {
            try ( PreparedStatement stmt = conn.prepareStatement("SELECT id, length FROM read WHERE seqrun_id=? AND name=?")) {
                stmt.setLong(1, runId);
                stmt.setString(2, seqName);
                try ( ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Sequence seq = new Sequence();
                        seq.setId(rs.getLong(1));
                        seq.setName(seqName);
                        seq.setLength(rs.getInt(2));
                        return seq;
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
        throw new MGXException("Not found.");
    }

    public AutoCloseableIterator<Long> getSeqIDs(long attrId) throws MGXException {
        try {
            long duration = System.currentTimeMillis();
            Connection conn = getConnection();
            duration = System.currentTimeMillis() - duration;
            if (duration > 1000) {
                getController().log("slow getConnection(), " + duration + "ms.");
            }

            duration = System.currentTimeMillis();

            PreparedStatement stmt = conn.prepareStatement("SELECT seq_id FROM observation WHERE attr_id=?");
            stmt.setFetchSize(50_000);
            stmt.setLong(1, attrId);
            ResultSet rs = stmt.executeQuery();

            duration = System.currentTimeMillis() - duration;
            if (duration > 1000) {
                getController().log("slow query for attribute id " + attrId + ", " + duration + "ms.");
            }

            return new DBIterator<Long>(rs, stmt, conn) {
                @Override
                public Long convert(ResultSet rs) throws SQLException {
                    return rs.getLong(1);
                }

            };
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    @Override
    public long create(Sequence obj) throws MGXException {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }
}
