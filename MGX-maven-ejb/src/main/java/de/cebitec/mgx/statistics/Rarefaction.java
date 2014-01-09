package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.JRI.RBool;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RList;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sj
 */
@Stateless(mappedName = "Rarefaction")
public class Rarefaction {

    @EJB
    R r;

    public AutoCloseableIterator<Point> rarefy(long[] data) throws MGXException {
        Rengine engine = r.getR();
        engine.eval("library(permute)");
        engine.eval("library(lattice)");
        engine.eval("library(vegan)");
        engine.eval(readScript());

        // convert to double
        double[] tmp = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            if (data[i] > Double.MAX_VALUE) {
                throw new MGXException("Could not convert " + data[i]);
            }
            tmp[i] = (double) data[i];
        }

        SecureRandom random = new SecureRandom();
        String rand = new BigInteger(130, random).toString(32);

        engine.assign("data" + rand, tmp);
        engine.eval("data" + rand + ".rare <- rarefaction(data.frame(t(data" + rand + ")), plot=F)");

        double[] subsamples = engine.eval("data" + rand + ".rare$subsample").asDoubleArray();
        double[] richness = engine.eval("unlist(data" + rand + ".rare$richness)").asDoubleArray();

        if (subsamples == null || richness == null) {
            throw new MGXException("Computing rarefaction failed.");
        }

        List<Point> ret = new LinkedList<>();
        for (int i = 0; i < subsamples.length; i++) {
            ret.add(new Point(subsamples[i], richness[i]));
        }

        engine.eval("rm(data" + rand + ")");
        engine.eval("rm(data" + rand + ".rare)");
        engine.end();

        return new ForwardingIterator<>(ret.iterator());
    }

    private String readScript() {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("de/cebitec/mgx/statistics/Rfunctions.r")) {
            try (InputStreamReader isr = new InputStreamReader(is)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not load R script: " + ex.getMessage());
        }
        return sb.toString();
    }

    private static String join(long[] data, String separator) {
        if (data.length == 0) {
            return "";
        }
        int idx = 0;
        StringBuilder oBuilder = new StringBuilder(String.valueOf(data[idx]));
        while (idx < data.length) {
            oBuilder.append(separator).append(data[++idx]);
        }
        return oBuilder.toString();
    }

}
