package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.Result;
import de.cebitec.mgx.model.db.GeneCoverage;
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
public class GeneCoverageDAO extends DAO<GeneCoverage> {

    public GeneCoverageDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class<GeneCoverage> getType() {
        return GeneCoverage.class;
    }

    private static final String BY_GENE = "SELECT gc.run_id, gc.coverage FROM gene g "
            + "LEFT JOIN gene_coverage gc ON (g.id=gc.gene_id) WHERE g.id=?";

    public Result<AutoCloseableIterator<GeneCoverage>> byGene(long gene_id) {

        List<GeneCoverage> l = null;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(BY_GENE)) {
                stmt.setLong(1, gene_id);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        return Result.error("No object of type Gene for ID " + gene_id + ".");
                    }
                    do {
                        if (rs.getLong(1) != 0) {
                            GeneCoverage ret = new GeneCoverage();
                            ret.setRunId(rs.getLong(1));
                            ret.setCoverage(rs.getInt(2));
                            ret.setGeneId(gene_id);

                            if (l == null) {
                                l = new ArrayList<>();
                            }
                            l.add(ret);
                        }
                    } while (rs.next());

                }

            }
        } catch (SQLException ex) {
            getController().log(ex);
            return Result.error(ex.getMessage());
        }
        ForwardingIterator<GeneCoverage> iter = new ForwardingIterator<>(l == null ? null : l.iterator());
        return Result.ok(iter);
    }

    @Override
    public Result<GeneCoverage> getById(long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long create(GeneCoverage obj) throws MGXException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
