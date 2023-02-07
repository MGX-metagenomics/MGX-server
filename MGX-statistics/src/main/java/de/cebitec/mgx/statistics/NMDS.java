package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.statistics.data.Matrix;
import de.cebitec.mgx.statistics.data.NamedVector;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
@Stateless(mappedName = "NMDS")
public class NMDS {

    @EJB
    Rserve r;
    //

    private static final String[] VEGDIST = new String[]{"manhattan",
        "euclidean", "canberra", "bray", "kulczynski",
        "jaccard", "gower", "altGower", "morisita", "horn",
        "mountford", "raup", "binomial", "chao", "cao",
        "mahalanobis"};

    public AutoCloseableIterator<Point> nmds(Matrix m) throws MGXException {
        return nmds(m, "bray");
    }

    public AutoCloseableIterator<Point> nmds(Matrix m, String distMethod) throws MGXException {
        if (m.getRows().size() < 3) {
            throw new MGXException("Insufficient number of datasets.");
        }
        if (!Util.contains(VEGDIST, distMethod)) {
            throw new MGXException("Invalid distance method: " + distMethod);
        }

        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        Map<String, String> sampleNames = new HashMap<>();
        Map<String, String> varNames = new HashMap<>();
        List<String> varOrder = new ArrayList<>();
        List<Point> ret = new LinkedList<>();

        try {
            int vecLen = -1;

            for (NamedVector nv : m.getRows()) {
                if (vecLen == -1) {
                    vecLen = nv.getData().length;
                }
                if (vecLen != nv.getData().length) {
                    throw new MGXException("Received vectors of different length.");
                }
                String varname = Util.generateSuffix("grp");
                sampleNames.put(varname, nv.getName());
                varOrder.add(varname);
                conn.assign(varname, nv.getData());
            }

            String matrixName = Util.generateSuffix("matr");
            conn.eval(String.format("%s <- rbind(%s)", matrixName, StringUtils.join(varOrder, ",")));

            int i = 0;
            String[] colAliases = new String[m.getColumnNames().length];
            for (String s : m.getColumnNames()) {
                String colName = Util.generateSuffix("var");
                varNames.put(colName, s);
                colAliases[i++] = colName;
            }
            String tmp = Util.generateSuffix("tmp.");
            conn.assign(tmp, colAliases);
            conn.eval(String.format("colnames(%s) <- %s", matrixName, tmp));
            conn.eval(String.format("rm(%s)", tmp));
            
            // we need to set a random seed for reproducible results
            conn.eval("set.seed(42)");

            String pcoaName = Util.generateSuffix("pcoa");
            conn.eval(String.format("x <- capture.output(%s <- metaMDS(%s, distance=\"%s\", k=2))", pcoaName, matrixName, distMethod));
            conn.eval("rm(x)");
            try {

                for (Entry<String, String> e : sampleNames.entrySet()) {
                    double[] coords = conn.eval(String.format("%s$points[\"%s\",]", pcoaName, e.getKey())).asDoubles();
                    Point p = new Point(coords[0], coords[1], e.getValue());
                    ret.add(p);
                }

                for (Entry<String, String> e : varNames.entrySet()) {
                    double[] coords = conn.eval(String.format("%s$species[\"%s\",]", pcoaName, e.getKey())).asDoubles();
                    Point p = new Point(coords[0], coords[1], e.getValue());
                    ret.add(p);
                }

            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new MGXException("Could not access requested components." + ex.getMessage());
            } finally {
                // cleanup
                for (String varName : sampleNames.keySet()) {
                    conn.eval(String.format("rm(%s)", varName));
                }
                conn.eval(String.format("rm(%s,%s)", pcoaName, matrixName));
            }

        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException(ex.getMessage());
        } finally {
            conn.close();
        }

        if (ret == null) {
            throw new MGXException("NMDS failed.");
        }

//        // re-convert group names
//        for (Map.Entry<String, String> e : names.entrySet()) {
//            nwk = nwk.replace(e.getKey(), e.getValue());
//        }
        return new ForwardingIterator<>(ret.iterator());
    }

}
