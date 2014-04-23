package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.misc.Matrix;
import de.cebitec.mgx.model.misc.NamedVector;
import de.cebitec.mgx.model.misc.PCAResult;
import de.cebitec.mgx.util.Point;
import de.cebitec.mgx.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "PCA")
public class PCA {

    @EJB
    Rserve r;
    //

    public PCAResult pca(Matrix m) throws MGXException {
        if (m.getRows().size() < 2) {
            throw new MGXException("Insufficient number of datasets.");
        }

        RConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to R.");
        }

        Map<String, String> sampleNames = new HashMap<>();
        Map<String, String> varNames = new HashMap<>();
        PCAResult ret = null;

        try {
            int vecLen = -1;

            for (NamedVector nv : m.getRows()) {
                if (vecLen == -1) {
                    vecLen = nv.getData().length;
                }
                if (vecLen != nv.getData().length) {
                    throw new MGXException("Received vectors of different length.");
                }
                String varname = "grp" + generateSuffix();
                sampleNames.put(varname, nv.getName());
                conn.assign(varname, toDoubleArray(nv.getData()));
            }

            String matrixName = "matr" + generateSuffix();
            //conn.eval(matrixName + " <- rbind(" + StringUtils.join(sampleNames.keySet(), ",") + ")");
            conn.eval(String.format("%s <- rbind(%s)", matrixName, StringUtils.join(sampleNames.keySet(), ","))); //matrixName + " <- rbind(" + StringUtils.join(sampleNames.keySet(), ",") + ")");

            int i = 0;
            String[] colAliases = new String[m.getColumnNames().length];
            for (String s : m.getColumnNames()) {
                String colName = "var" + generateSuffix();
                varNames.put(colName, s);
                colAliases[i++] = colName;
            }
            String tmp = "tmp." + generateSuffix();
            conn.assign(tmp, colAliases);
            conn.eval(String.format("colnames(%s) <- %s", matrixName, tmp));
            conn.eval(String.format("rm(%s)", tmp));

            String pcaName = "pca" + generateSuffix();
            conn.eval(String.format("%s <- try(prcomp(%s, scale=T), silent=T)", pcaName, matrixName));

            // if PCA fails, e.g. due to 0 variance in a column, try without scaling
            conn.eval(String.format("if (class(%s) == \"try-error\") { %s <- prcomp(%s, scale=F) }", pcaName, pcaName, matrixName));

            // get variances
            double[] variances = conn.eval(pcaName + "$sdev^2").asDoubles();
            ret = new PCAResult(variances);

            for (Entry<String, String> e : sampleNames.entrySet()) {
                double[] coords = conn.eval(String.format("%s$x[\"%s\",1:2]", pcaName, e.getKey())).asDoubles();
                Point p = new Point(coords[0], coords[1], e.getValue());
                ret.addPoint(p);
            }

            // fetch loadings
            for (Entry<String, String> e : varNames.entrySet()) {
                double[] coords = conn.eval(String.format("%s$rotation[\"%s\",1:2]", pcaName, e.getKey())).asDoubles();
                Point p = new Point(coords[0], coords[1], e.getValue());
                ret.addLoading(p);
            }

            // cleanup
            for (String varName : sampleNames.keySet()) {
                conn.eval(String.format("rm(%s)", varName));
            }
            conn.eval(String.format("rm(%s)", matrixName));

        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret == null) {
            throw new MGXException("PCA failed.");
        }

//        // re-convert group names
//        for (Map.Entry<String, String> e : names.entrySet()) {
//            nwk = nwk.replace(e.getKey(), e.getValue());
//        }
        return ret;
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
