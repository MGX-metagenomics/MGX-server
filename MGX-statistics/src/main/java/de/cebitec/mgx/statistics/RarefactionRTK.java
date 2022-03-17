package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.LinkedList;
import java.util.List;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sj
 */
@Stateless(mappedName = "RarefactionRTK")
public class RarefactionRTK {

    private final static int DEFAULT_NUM_DATAPOINTS = 50;

    @EJB
    Rserve r;

    public AutoCloseableIterator<Point> rarefy(double[] data) throws MGXException {
        RWrappedConnection conn = r.getR();
        List<Point> ret = new LinkedList<>();

        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        int sum = 0;
        for (double d : data) {
            sum += d;
        }

        try {
            // create unique variable name
            String name = Util.generateSuffix("data");

            conn.assign(name, data);
            REXP ev = conn.eval(name + ".rare <- rtk(as.matrix(" + name + "), repeats=50, ReturnMatrix=0, depth=round(seq.int(1, " + sum + ", length.out="
                    + Math.min(DEFAULT_NUM_DATAPOINTS, sum) + ")), verbose=F)");
            if (ev != null) {
                double[] subsamples = conn.eval(name + ".rare$depths").asDoubles();
                double[] richness = conn.eval("unlist(lapply(" + name + ".rare$depths, function(idx) { " + name + ".rare[[as.character(idx)]]$div.mean$mean.richness }))").asDoubles();
                conn.eval("rm(" + name + "," + name + ".rare)");

                if (subsamples == null || richness == null) {
                    throw new MGXException("Computing RTK rarefaction failed.");
                }

                for (int i = 0; i < subsamples.length; i++) {
                    Point p = new Point(subsamples[i], richness[i]);
                    ret.add(p);
                }
            }

        } catch (REXPMismatchException | REngineException ex) {
            throw new MGXException("Computing RTK rarefaction failed: " + ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret.isEmpty()) {
            throw new MGXException("Computing RTK rarefaction failed, no results.");
        }

        return new ForwardingIterator<>(ret.iterator());
    }

}
