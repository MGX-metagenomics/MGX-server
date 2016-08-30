package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXControllerImpl;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class SequenceDAO<T extends Sequence> extends DAO<T> {

    private static final String GET_SEQRUN = "SELECT s.dbfile, r.name FROM seqrun s RIGHT JOIN read r ON (s.id = r.seqrun_id) WHERE r.id=?";

    public SequenceDAO(MGXControllerImpl ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Sequence.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getById(Long id) throws MGXException {

        // find the storage file for this sequence
        String dbFile = null;
        String seqName = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(GET_SEQRUN)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        dbFile = rs.getString(1);
                        seqName = rs.getString(2);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        if (dbFile == null || dbFile.isEmpty() || seqName == null || seqName.isEmpty()) {
            throw new MGXException("No sequence for ID " + id);
        }

        Sequence seq = new Sequence();
        seq.setId(id);
        seq.setName(seqName);

        // read sequence data
        try (SeqReaderI<? extends DNASequenceI> reader = SeqReaderFactory.<DNASequenceI>getReader(dbFile)) {
            Iterator<? extends DNASequenceI> iter = reader.fetch(new long[]{id}).iterator();
            if (iter.hasNext()) {
                DNASequenceI dnaSeq = iter.next();
                byte[] seqdata = dnaSeq.getSequence();
                String seqString = new String(seqdata).toUpperCase();
                seq.setSequence(seqString);
                seq.setLength(seqString.length());
            }
        } catch (Exception ex) {
            throw new MGXException(ex);
        }
        return (T) seq;
    }

    @Override
    public AutoCloseableIterator<T> getByIds(Collection<Long> ids) throws MGXException {
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

            return new DBIterator<T>(rs, stmt, conn) {
                @Override
                public T convert(ResultSet rs) throws SQLException {
                    Sequence seq = new Sequence();
                    seq.setId(rs.getLong(1));
                    seq.setName(rs.getString(2));
                    seq.setLength(rs.getInt(3));
                    return (T) seq;
                }
            };
        } catch (SQLException ex) {
            getController().log("SQL statement failed: " + GET_BY_IDS);
            throw new MGXException(ex);
        }

    }

    public Sequence byName(long runId, String seqName) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, length FROM read WHERE seqrun_id=? AND name=?")) {
                stmt.setLong(1, runId);
                stmt.setString(2, seqName);
                try (ResultSet rs = stmt.executeQuery()) {
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
            throw new MGXException(ex);
        }
        throw new MGXException("Not found.");
    }

    public AutoCloseableIterator<Long> getSeqIDs(long attrId) throws MGXException {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT seq_id FROM observation WHERE attr_id=?");
            stmt.setLong(1, attrId);
            ResultSet rs = stmt.executeQuery();
            return new DBIterator<Long>(rs, stmt, conn) {
                @Override
                public Long convert(ResultSet rs) throws SQLException {
                    return rs.getLong(1);
                }
            };
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }
}
