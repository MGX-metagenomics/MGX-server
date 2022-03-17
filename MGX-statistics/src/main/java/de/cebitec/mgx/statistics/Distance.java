package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "Distance")
public class Distance {

    @EJB
    Rserve r;
    //

    public double aitchisonDistance(double[] d1, double[] d2) throws MGXException {

        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        double ret = -1;

        try {
            String var1 = Util.generateSuffix("grp");
            conn.assign(var1, d1);

            String var2 = Util.generateSuffix("grp");
            conn.assign(var2, d2);

            String matrixName = Util.generateSuffix("matr");
            conn.eval(String.format("%s <- rbind(%s, %s)", matrixName, var1, var2));

//            conn.eval(String.format("%s <- aitchisonAdditiveZeroReplacement(%s)", var2, var2));
//            conn.eval(String.format("%s <- aitchisonAdditiveZeroReplacement(%s)", var1, var1));

            String distName = Util.generateSuffix("d");
            conn.eval(String.format("%s <- aitchisonDist(%s)", distName, matrixName));
            try {
                ret = conn.eval(String.format("%s[1]", distName)).asDouble();
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new MGXException("Could not access requested component." + ex.getMessage());
            } finally {
                conn.eval(String.format("rm(%s, %s, %s, %s)", distName, matrixName, var1, var2));
            }

        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret == -1) {
            throw new MGXException("Aitchison distance computation failed.");
        }

        return ret;
    }

}
