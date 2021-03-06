package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sj
 */
@Stateless(mappedName = "Rarefaction")
public class Rarefaction {

    @EJB
    Rserve r;

    public AutoCloseableIterator<Point> rarefy(double[] data) throws MGXException {
        RWrappedConnection conn = r.getR();
        List<Point> ret = new LinkedList<>();

        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        try {
            // create unique variable name
            String name = Util.generateSuffix("data");

            float d = 0;
            for (double x : data) {
                d += x;
            }
            int subsample = 5;
            if (d > 100) {
                subsample = Math.max(1, Math.round(d / 75f));
            }

            conn.assign(name, data);
            REXP ev = conn.eval(name + ".rare <- rarefaction(data.frame(t(" + name + ")), subsample=" + subsample + ")");
            if (ev != null) {
                double[] subsamples = conn.eval(name + ".rare$subsample").asDoubles();
                double[] richness = conn.eval(name + ".rare$richness").asDoubles();

                if (subsamples == null || richness == null) {
                    throw new MGXException("Computing rarefaction failed.");
                }

                for (int i = 0; i < subsamples.length; i++) {
                    Point p = new Point(subsamples[i], richness[i]);
                    ret.add(p);
                }
            }

            // cleanup variables
            conn.eval("rm(" + name + ")");
            conn.eval("rm(" + name + ".rare)");
        } catch (REXPMismatchException | REngineException ex) {
            throw new MGXException("Computing rarefaction failed: " + ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret.isEmpty()) {
            throw new MGXException("Computing rarefaction failed, no results.");
        }

        return new ForwardingIterator<>(ret.iterator());
    }

}
