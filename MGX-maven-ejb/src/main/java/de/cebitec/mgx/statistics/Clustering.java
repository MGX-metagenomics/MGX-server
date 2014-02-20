package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.misc.NamedVector;
import de.cebitec.mgx.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "Clustering")
public class Clustering {

    @EJB
    Rserve r;
    //
    private static final String[] AGGLO = new String[]{"ward", "single", "complete", "average", "mcquitty", "median", "centroid"};
    private static final String[] DIST = new String[]{"euclidean", "maximum", "manhattan", "canberra", "binary", "minkowski"};

    public String cluster(Set<NamedVector> data, String distMethod, String aggloMethod) throws MGXException {
        if (data.size() < 2) {
            throw new MGXException("Insufficient number of datasets.");
        }

        if (!contains(DIST, distMethod)) {
            throw new MGXException("Invalid distance method: " + distMethod);
        }
        if (!contains(AGGLO, aggloMethod)) {
            throw new MGXException("Invalid agglomeration method: " + aggloMethod);
        }

        RConnection conn = r.getR();
        Map<String, String> names = new HashMap<>();
        String nwk = null;

        try {
            int vecLen = -1;

            for (NamedVector nv : data) {
                if (vecLen == -1) {
                    vecLen = nv.getData().length;
                }
                if (vecLen != nv.getData().length) {
                    throw new MGXException("Received vectors of different length.");
                }
                String varname = "grp" + generateSuffix();
                names.put(varname, nv.getName());
                conn.assign(varname, toDoubleArray(nv.getData()));
            }

            String matrixName = "matr" + generateSuffix();
            conn.eval(matrixName + " <- rbind(" + StringUtils.join(names.keySet(), ",") + ")");

            String stmt = String.format("ctc::hc2Newick(hclust(dist(scale(%s),method=\"%s\"),method=\"%s\"))", matrixName, distMethod, aggloMethod);
            nwk = conn.eval(stmt).asString();

            // cleanup
            for (String varname : names.keySet()) {
                conn.eval("rm(" + varname + ")");
            }
            conn.eval("rm(" + matrixName + ")");

        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        // re-convert group names
        for (Map.Entry<String, String> e : names.entrySet()) {
            nwk = nwk.replace(e.getKey(), e.getValue());
        }

        return nwk;
    }

    private static String generateSuffix() {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    private double[] toDoubleArray(long[] in) {
        double[] ret = new double[in.length];
        int i = 0;
        for (long l : in) {
            ret[i++] = (double) l;
        }
        return ret;
    }

    private static boolean contains(String[] options, String value) {
        for (String o : options) {
            if (o.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
