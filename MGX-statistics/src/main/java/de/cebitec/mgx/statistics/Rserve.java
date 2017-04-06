package de.cebitec.mgx.statistics;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.streamlogger.StreamLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 *
 * @author sj
 */
@Singleton(mappedName = "Rserve")
@Startup
public class Rserve {

    private static volatile boolean exiting = false;

    public synchronized RWrappedConnection getR() {
        // do not offer new connections during shutdown/undeploy
        if (exiting) {
            return null;
        }
        RConnection ret = null;
        try {
            ret = new RConnection();
        } catch (RserveException ex) {
            try {
                ret = new RConnection();
            } catch (RserveException ex1) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        if (ret == null) {
            return null;
        }
        try {
            return new RWrappedConnection(ret);
        } catch (RserveException ex) {
            Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private File scriptFile = null;
    private Process p = null;
    private Thread err = null;
    private Thread out = null;

    // "typical" locations for the R installation; this currently reflects the
    // environment found in Bielefeld and Giessen
    private final static String[] Rbinaries = new String[]{"/vol/r-2.15/bin/R", "/vol/mgx-sw/bin/R", "/usr/bin/R"};

    @PostConstruct
    public void start() {
        try {

            // check for a local R installation
            File R = null;
            for (String maybeR : Rbinaries) {
                File f = new File(maybeR);
                if (f.exists() && f.canExecute()) {
                    R = f;
                    break;
                }
            }
            if (R == null) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, "No R installation found.");
                return;
            }

            scriptFile = createScript();

            String rserveStartCommand = R.getAbsolutePath() + " CMD Rserve -q --vanilla --RS-source " + scriptFile.getAbsolutePath();
            p = Runtime.getRuntime().exec(rserveStartCommand);
            //
            err = new StreamLogger("R error logger thread", p.getErrorStream(), Logger.getLogger("Rserv-stderr"));
            err.start();

            out = new StreamLogger("R output logger thread", p.getInputStream(), Logger.getLogger("Rserv-stdout"));
            out.start();
            //
            //p.waitFor();
        } catch (IOException | MGXException ex) {
            Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void stop() {
        exiting = true;
        if (p != null) {
            RConnection conn = null;
            try {
                conn = new RConnection();
            } catch (RserveException ex) {
            }
            if (conn != null) {
                try {
                    conn.shutdown();
                    conn.close();
                } catch (RserveException ex) {
                    Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            p.destroy();
        }

        if (scriptFile != null && scriptFile.exists()) {
            scriptFile.delete();
        }

        if (err != null) {
            err.interrupt();
        }
        if (out != null) {
            out.interrupt();
        }
        if (err != null) {
            try {
                err.join(); 
            } catch (InterruptedException ex) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (out != null) {
            try {
                out.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private synchronized File createScript() throws MGXException {
        if (scriptFile != null) {
            return scriptFile;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("de/cebitec/mgx/statistics/Rfunctions.r")) {
            File tmpRFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), "RSource.R");
            Files.copy(is, tmpRFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tmpRFile;
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }

}
