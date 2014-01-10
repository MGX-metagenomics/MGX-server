package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "R")
@Startup
public class R {

    private final static Logger logger = Logger.getLogger(R.class.getPackage().getName());
    private Rengine re = null;

    public Rengine getR() throws MGXException {
        re = Rengine.getMainEngine();
        if (re == null) {
            re = new Rengine(new String[]{("--vanilla"), ("--silent")}, false, new RLogger());
            if (!re.waitForR()) {
                throw new MGXException("Error creating R instance");
            }
        }
        re.assign("tmpdir", System.getProperty("java.io.tmpdir"));
        re.eval(readScript());

        return re;
    }

    @PostConstruct
    public void start() {
        if (!Rengine.versionCheck()) {
            throw new RuntimeException("** R Version mismatch - Java files don't match library version.");
        }
    }

    @PreDestroy
    public void stop() {
        if (re != null) {
            re.end();
            re = null;
        }
    }

    private String readScript() throws MGXException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("de/cebitec/mgx/statistics/Rfunctions.r")) {
            try (InputStreamReader isr = new InputStreamReader(is)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    }
                }
            }
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
        return sb.toString();
    }
}
