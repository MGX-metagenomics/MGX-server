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
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "Clustering")
public class Clustering {

    @EJB
    R r;

    public String cluster(Set<NamedVector> data) throws MGXException {
        if (data.size() < 2) {
            throw new MGXException("Insufficient number of datasets.");
        }
        Rengine engine = r.getR();

        int vecLen = -1;
        Map<String, String> names = new HashMap<>();

        for (NamedVector nv : data) {
            if (vecLen == -1) {
                vecLen = nv.getData().length;
            }
            if (vecLen != nv.getData().length) {
                throw new MGXException("Received vectors of different length.");
            }
            String varname = "grp" + generateSuffix();
            names.put(varname, nv.getName());
            engine.assign(varname, toDoubleArray(nv.getData()));
        }

        String matrixName = "matr" + generateSuffix();
        engine.eval(matrixName + " <- rbind(" + StringUtils.join(names.keySet(), ",") + ")");
        
        String nwk = engine.eval("ctc::hc2Newick(hclust(dist(scale(" + matrixName + "))))").asString();

        // cleanup
        for (String varname : names.keySet()) {
            engine.eval("rm(" + varname + ")");
        }
        engine.eval("rm(" + matrixName + ")");
        engine.end();

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
}
