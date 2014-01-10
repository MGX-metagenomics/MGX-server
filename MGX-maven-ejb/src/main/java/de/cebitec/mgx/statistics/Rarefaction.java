package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.Point;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sj
 */
@Stateless(mappedName = "Rarefaction")
public class Rarefaction {

    @EJB
    R r;

    public AutoCloseableIterator<Point> rarefy(double[] data) throws MGXException {
        Rengine engine = r.getR();

        // create unique variable name
        String name = "data" + generateSuffix();

        engine.assign(name, data);
        engine.eval(name + ".rare <- rarefaction(data.frame(t(" + name + ")))");

        double[] subsamples = engine.eval(name + ".rare$subsample").asDoubleArray();
        double[] richness = engine.eval(name + ".rare$richness").asDoubleArray();

        engine.eval("print (" + name + ".rare)");
        if (subsamples == null || richness == null) {
            throw new MGXException("Computing rarefaction failed.");
        }

        List<Point> ret = new LinkedList<>();
        for (int i = 0; i < subsamples.length; i++) {
            Point p = new Point(subsamples[i], richness[i]);
            ret.add(p);
            System.err.println(p.getX() + " / " + p.getY());
        }

        // cleanup variables
        engine.eval("rm(" + name + ")");
        engine.eval("rm(" + name + ".rare)");
        engine.end();

        return new ForwardingIterator<>(ret.iterator());
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

}
