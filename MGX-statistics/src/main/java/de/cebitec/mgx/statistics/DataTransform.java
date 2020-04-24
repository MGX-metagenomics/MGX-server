package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "DataTransform")
public class DataTransform {

    @EJB
    Rserve r;
    //

    public double[] clr(final double[] data) throws MGXException {

        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        double[] clr = null;

        try {
            String name = Util.generateSuffix("data");
            conn.assign(name, data);
            REXP ev = conn.eval(name + ".clr <- clr(" + name + ")");
            if (ev != null) {
                clr = conn.eval(name + ".clr").asDoubles();
            }
            conn.eval("rm(" + name + ")");
            conn.eval("rm(" + name + ".clr)");
        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        if (clr == null) {
            throw new MGXException("clr transform failed.");
        }
        if (clr.length != data.length) {
            throw new MGXException("clr transform returned invalid result.");
        }

        return clr;
    }

}
