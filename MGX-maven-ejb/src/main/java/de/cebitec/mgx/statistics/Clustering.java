package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.misc.NamedVector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

        // ordered list of R variable names
        List<String> varnames = new LinkedList<>();
        varnames.addAll(names.keySet());

        String matrixName = "matr" + generateSuffix();
        engine.assign(matrixName, "rbind(" + join(varnames, ",") + ")");

        // cleanup
        for (String varname : names.keySet()) {
            engine.eval("rm(" + varname + ")");
        }
        engine.eval("rm(" + matrixName + ")");
        engine.end();

        return "FIXME";
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

    /*
     * from http://snippets.dzone.com/posts/show/91
     */
    protected static String join(Iterable< ? extends Object> pColl, String separator) {
        Iterator< ? extends Object> oIter;
        if (pColl == null || (!(oIter = pColl.iterator()).hasNext())) {
            return "";
        }
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext()) {
            oBuilder.append(separator).append(oIter.next());
        }
        return oBuilder.toString();
    }
}
