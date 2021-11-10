package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.newick.NewickParser;
import de.cebitec.mgx.newick.ParserException;
import de.cebitec.mgx.statistics.data.Matrix;
import de.cebitec.mgx.statistics.data.NamedVector;
import de.cebitec.mgx.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "Clustering")
public class Clustering {

    @EJB
    Rserve r;
    @EJB
    DataTransform transform;
    //
    private static final String[] AGGLO = new String[]{"ward", "single", "complete", "average", "mcquitty", "median", "centroid"};
    private static final String[] DIST = new String[]{"aitchison", "euclidean", "maximum", "manhattan", "canberra", "binary", "minkowski"};

    public String cluster(Matrix m, String distMethod, String aggloMethod) throws MGXException {
        if (m.getRows().size() < 2) {
            throw new MGXException("Insufficient number of datasets.");
        }

        if (!Util.contains(DIST, distMethod)) {
            throw new MGXException("Invalid distance method: " + distMethod);
        }
        if (!Util.contains(AGGLO, aggloMethod)) {
            throw new MGXException("Invalid agglomeration method: " + aggloMethod);
        }

        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }

        Map<String, String> names = new HashMap<>();
        String nwk = null;

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
                names.put(varname, nv.getName());

                double[] data = nv.getData();
                if (distMethod.equals("aitchison")) {
                    data = transform.clr(nv.getData());
                }
                conn.assign(varname, data);
            }

            String matrixName = Util.generateSuffix("matr");
            conn.eval(matrixName + " <- rbind(" + StringUtils.join(names.keySet(), ",") + ")");

            if (distMethod.equals("aitchison")) {
                distMethod = "euclidean";
            }

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

        if (nwk == null) {
            throw new MGXException("Clustering failed.");
        }

        /*
         * Clustering might fail e.g. when two equal datasets are present 
         * since their distance is 0. Try to extract and report a meaningful
         * error message
         */
        if (nwk.contains("Error in")) {
            LOG.log(Level.SEVERE, nwk);
            String err = "Unknown error.";
            if (nwk.contains(":")) {
                err = nwk.substring(nwk.lastIndexOf(":"));
                err = err.replaceAll("\n", ""); // remove line breaks
                if (err.contains(":")) {
                    err = err.substring(err.lastIndexOf(":"));
                }
                if (err.contains("(")) {
                    err = err.substring(0, err.lastIndexOf("("));
                }
                if (err.startsWith(": ")) {
                    err = err.substring(2);
                }
                while (err.contains("  ")) {
                    err = err.replaceAll("  ", " ");
                }
            }
            throw new MGXException("Could not cluster data: " + err.trim());
        }

        // re-convert group names
        for (Map.Entry<String, String> e : names.entrySet()) {
            nwk = nwk.replace(e.getKey(), e.getValue());
        }

        return nwk;
    }

    public String newickToSVG(String newick) throws MGXException {
        
        try {
            NewickParser.parse(newick);
        } catch (ParserException ex) {
            throw new MGXException("Unable to parse Newick string: " + ex.getMessage());
        }
        
        String svgString;
        RWrappedConnection conn = r.getR();
        if (conn == null) {
            throw new MGXException("Could not connect to Rserve.");
        }
        try {
            svgString = conn.eval(String.format("create_nwk_tree(\"%s\")", newick)).asString();
        } catch (REngineException | REXPMismatchException ex) {
            throw new MGXException("SVG conversion failed: " + ex.getMessage());
        } finally {
            conn.close();
        }
        return svgString;
    }
    
    private static final Logger LOG = Logger.getLogger(Clustering.class.getName());

}
