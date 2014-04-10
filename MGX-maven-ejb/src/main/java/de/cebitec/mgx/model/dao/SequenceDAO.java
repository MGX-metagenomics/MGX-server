package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class SequenceDAO<T extends Sequence> extends DAO<T> {

    private static final String GET_SEQRUN = "SELECT s.dbfile, r.name FROM seqrun s RIGHT JOIN read r ON (s.id = r.seqrun_id) WHERE r.id=?";

    @Override
    Class getType() {
        return Sequence.class;
    }

    @Override
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

        if (dbFile == null || seqName == null) {
            throw new MGXException("No sequence for ID "+ id);
        }
        
        T seq = (T) new Sequence();
        seq.setId(id);
        seq.setName(seqName);

        // read sequence data
        try (SeqReaderI<DNASequenceHolder> reader = SeqReaderFactory.getReader(dbFile)) {
            Iterator<DNASequenceHolder> iter = reader.fetch(new long[]{id}).iterator();
            assert iter.hasNext();
            byte[] seqdata = iter.next().getSequence().getSequence();
            String seqString = new String(seqdata);
            seq.setSequence(seqString);
            seq.setLength(seqString.length());
        } catch (Exception ex) {
            throw new MGXException(ex);
        }
        return seq;
    }
}
