package de.cebitec.mgx.statistics;

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

    public Rengine getR() {
        re = Rengine.getMainEngine();
        if (re == null) {
            re = new Rengine(new String[]{("--vanilla"), ("--silent")}, false, new RLogger());
            if (!re.waitForR()) {
                throw new RuntimeException("Error waiting for R!");
            }
        }
        re.assign("tmpdir", System.getProperty("java.io.tmpdir"));
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
}
