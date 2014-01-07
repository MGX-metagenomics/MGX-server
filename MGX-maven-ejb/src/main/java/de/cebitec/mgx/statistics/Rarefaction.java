package de.cebitec.mgx.statistics;

import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.Point;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import javax.ejb.EJB;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sj
 */
@EJB
public class Rarefaction {

    @EJB
    R r;

    public AutoCloseableIterator<Point> rarefy(int[] data) {
        Rengine engine = r.getR();
        engine.eval("library(vegan)");
        engine.eval(readScript());
        UUID name = UUID.randomUUID();
        engine.eval(name.toString() + " <- c(" + join(data, ",") + ")");
        engine.eval("rm(" + name.toString() + ")");
        engine.end();
        return new ForwardingIterator<>(null);
    }
    
    private String readScript() {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("")) {
            try (InputStreamReader isr = new InputStreamReader(is)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    sb.append(br.readLine());
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return sb.toString();
    }

    private static String join(int[] data, String separator) {
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
