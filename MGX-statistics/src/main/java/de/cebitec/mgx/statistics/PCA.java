package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.statistics.data.Matrix;
import de.cebitec.mgx.statistics.data.NamedVector;
import de.cebitec.mgx.statistics.data.PCAResult;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "PCA")
public class PCA {

    @EJB
    Rserve r;
    //

    public PCAResult pca(Matrix m, int pc1, int pc2) throws MGXException {
        if (m.getRows().size() < 2) {
            throw new MGXException("Insufficient number of datasets.");
        }

        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        Map<String, String> sampleNames = new HashMap<>();
        Map<String, String> varNames = new HashMap<>();
        List<String> varOrder = new ArrayList<>();
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
                String varname = "grp" + Util.generateSuffix();
                sampleNames.put(varname, nv.getName());
                varOrder.add(varname);
                conn.assign(varname, nv.getData());
            }

            String matrixName = "matr" + Util.generateSuffix();
            conn.eval(String.format("%s <- rbind(%s)", matrixName, StringUtils.join(varOrder, ",")));

            int i = 0;
            String[] colAliases = new String[m.getColumnNames().length];
            for (String s : m.getColumnNames()) {
                String colName = "var" + Util.generateSuffix();
                varNames.put(colName, s);
                colAliases[i++] = colName;
            }
            String tmp = "tmp." + Util.generateSuffix();
            conn.assign(tmp, colAliases);
            conn.eval(String.format("colnames(%s) <- %s", matrixName, tmp));
            conn.eval(String.format("rm(%s)", tmp));

            String pcaName = "pca" + Util.generateSuffix();
            conn.eval(String.format("%s <- try(prcomp(%s, scale=T), silent=T)", pcaName, matrixName));

            // if PCA fails, e.g. due to 0 variance in a column, try without scaling
            conn.eval(String.format("if (class(%s) == \"try-error\") { %s <- prcomp(%s, scale=F) }", pcaName, pcaName, matrixName));

            // get variances
            double[] variances = conn.eval(pcaName + "$sdev^2").asDoubles();
            ret = new PCAResult(variances);

            try {

                for (Entry<String, String> e : sampleNames.entrySet()) {
                    double[] coords = conn.eval(String.format("%s$x[\"%s\",]", pcaName, e.getKey())).asDoubles();
                    Point p = new Point(coords[pc1 - 1], coords[pc2 - 1], e.getValue());
                    ret.addPoint(p);
                }

                // fetch loadings
                for (Entry<String, String> e : varNames.entrySet()) {
                    double[] coords = conn.eval(String.format("%s$rotation[\"%s\",]", pcaName, e.getKey())).asDoubles();
                    Point p = new Point(coords[pc1 - 1], coords[pc2 - 1], e.getValue());
                    ret.addLoading(p);
                }

            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new MGXException("Could not access requested principal components.");
            } finally {
                // cleanup
                for (String varName : sampleNames.keySet()) {
                    conn.eval(String.format("rm(%s)", varName));
                }
                conn.eval(String.format("rm(%s)", pcaName));
                conn.eval(String.format("rm(%s)", matrixName));
            }

        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret == null) {
            throw new MGXException("PCA failed.");
        }
        return ret;
    }
}
