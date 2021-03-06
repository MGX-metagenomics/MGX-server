package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class DNAExtractDAO extends DAO<DNAExtract> {

    public DNAExtractDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return DNAExtract.class;
    }

    private final static String CREATE = "INSERT INTO dnaextract (sample_id, name, description, fiveprimer, method, protocol, targetfragment, targetgene, threeprimer) "
            + "VALUES (?,?,?,?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(DNAExtract obj) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setLong(1, obj.getSampleId());
                stmt.setString(2, obj.getName());
                stmt.setString(3, obj.getDescription());
                stmt.setString(4, obj.getFivePrimer());
                stmt.setString(5, obj.getMethod());
                stmt.setString(6, obj.getProtocol());
                stmt.setString(7, obj.getTargetFragment());
                stmt.setString(8, obj.getTargetGene());
                stmt.setString(9, obj.getThreePrimer());

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

    private final static String UPDATE = "UPDATE dnaextract SET name=?, description=?, fiveprimer=?, method=?, protocol=?, "
            + "targetfragment=?, targetgene=?, threeprimer=? "
            + "WHERE id=?";

    public void update(DNAExtract obj) throws MGXException {
        if (obj.getId() == DNAExtract.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getName());
                stmt.setString(2, obj.getDescription());
                stmt.setString(3, obj.getFivePrimer());
                stmt.setString(4, obj.getMethod());
                stmt.setString(5, obj.getProtocol());
                stmt.setString(6, obj.getTargetFragment());
                stmt.setString(7, obj.getTargetGene());
                stmt.setString(8, obj.getThreePrimer());

                stmt.setLong(9, obj.getId());
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + obj.getId() + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    public void delete(long id) throws MGXException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM dnaextract WHERE id=?")) {
                stmt.setLong(1, id);
                int numRows = stmt.executeUpdate();
                if (numRows != 1) {
                    throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private final static String BY_ID = "SELECT id, name, description, fiveprimer, method, protocol, "
            + "targetfragment, targetgene, threeprimer, sample_id "
            + "FROM dnaextract WHERE id=?";

    @Override
    public DNAExtract getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_ID)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }
                    DNAExtract d = new DNAExtract();
                    d.setId(rs.getLong(1));
                    d.setName(rs.getString(2));
                    d.setDescription(rs.getString(3));
                    d.setFivePrimer(rs.getString(4));
                    d.setMethod(rs.getString(5));
                    d.setProtocol(rs.getString(6));
                    d.setTargetFragment(rs.getString(7));
                    d.setTargetGene(rs.getString(8));
                    d.setThreePrimer(rs.getString(9));
                    d.setSampleId(rs.getLong(10));

                    return d;
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }
    }

    private final static String FETCHALL = "SELECT d.id, d.name, d.description, d.fiveprimer, d.method, d.protocol, "
            + "d.targetfragment, d.targetgene, d.threeprimer, d.sample_id "
            + "FROM dnaextract d";

    public AutoCloseableIterator<DNAExtract> getAll() throws MGXException {
        List<DNAExtract> ret = null;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(FETCHALL)) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {

                        if (ret == null) {
                            ret = new ArrayList<>();
                        }
                        DNAExtract d = new DNAExtract();
                        d.setId(rs.getLong(1));
                        d.setName(rs.getString(2));
                        d.setDescription(rs.getString(3));
                        d.setFivePrimer(rs.getString(4));
                        d.setMethod(rs.getString(5));
                        d.setProtocol(rs.getString(6));
                        d.setTargetFragment(rs.getString(7));
                        d.setTargetGene(rs.getString(8));
                        d.setThreePrimer(rs.getString(9));
                        d.setSampleId(rs.getLong(10));

                        ret.add(d);
                    }
                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }

    private final static String SQL_BY_SAMPLE = "SELECT d.id, d.name, d.description, d.fiveprimer, d.method, d.protocol, "
            + "d.targetfragment, d.targetgene, d.threeprimer "
            + "FROM sample s LEFT JOIN dnaextract d ON (s.id=d.sample_id) WHERE s.id=?";

    public AutoCloseableIterator<DNAExtract> bySample(final long sample_id) throws MGXException {
        if (sample_id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        List<DNAExtract> ret = null;

//        final Sample sample = getController().getSampleDAO().getById(sample_id);

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BY_SAMPLE)) {
                stmt.setLong(1, sample_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new MGXException("No object of type Sample for ID " + sample_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            DNAExtract d = new DNAExtract();
                            d.setSampleId(sample_id);
                            d.setId(rs.getLong(1));
                            d.setName(rs.getString(2));
                            d.setDescription(rs.getString(3));
                            d.setFivePrimer(rs.getString(4));
                            d.setMethod(rs.getString(5));
                            d.setProtocol(rs.getString(6));
                            d.setTargetFragment(rs.getString(7));
                            d.setTargetGene(rs.getString(8));
                            d.setThreePrimer(rs.getString(9));

                            if (ret == null) {
                                ret = new ArrayList<>();
                            }
                            ret.add(d);
                        }
                    } while (rs.next());

                }
            }
        } catch (SQLException ex) {
            getController().log(ex);
            throw new MGXException(ex);
        }

        return new ForwardingIterator<>(ret == null ? null : ret.iterator());
    }
}
