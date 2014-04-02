package de.cebitec.mgx.statistics;

import de.cebitec.mgx.controller.MGXException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
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

    public RConnection getR() {
        RConnection ret = null;
        try {
            ret = new RConnection();
        } catch (RserveException ex) {
            start();
            try {
                ret = new RConnection();
            } catch (RserveException ex1) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        if (ret == null) {
            return ret;
        }
        try {
            //ret.eval("options(error=function() NULL)"); //disable exit on error
            //REXP ev = ret.eval("source('" + scriptFile.getAbsolutePath() + "')");
            return new RWrappedConnection(ret);
        } catch (RserveException ex) {
            Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    private File scriptFile = null;
    private Process p = null;
    private Thread err = null;
    private Thread out = null;

    @PostConstruct
    public void start() {
        try {
            scriptFile = createScript();
            String rserveStartCommand = "/vol/r-2.15/bin/R CMD Rserve --vanilla --RS-source " + scriptFile.getAbsolutePath();
            p = Runtime.getRuntime().exec(rserveStartCommand);
            //
            err = new Thread(new RStreamReader(p.getErrorStream()));
            err.setName("R error logger thread");
            err.start();

            out = new Thread(new RStreamReader(p.getInputStream()));
            out.setName("R output logger thread");
            out.start();
            //
            p.waitFor();
        } catch (IOException | InterruptedException | MGXException ex) {
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
                }
            }
            try {
                p.getInputStream().close();
                p.getErrorStream().close();
            } catch (IOException ex) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
            }
            p.destroy();
        }

        if (scriptFile != null && scriptFile.exists()) {
            scriptFile.delete();
        }
        if (err != null) {
            err.interrupt();
            try {
                err.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (out != null) {
            out.interrupt();
            try {
                out.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String generateSuffix() {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    private synchronized File createScript() throws MGXException {
        if (scriptFile != null) {
            return scriptFile;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("de/cebitec/mgx/statistics/Rfunctions.r")) {
            StringBuffer tmpRFileName = new StringBuffer('.').append("RSource").append(".R");
            File tmpRFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), tmpRFileName.toString());

            byte[] buffer = new byte[1024];
            try (FileOutputStream rOut = new FileOutputStream(tmpRFile)) {
                int bytesRead = is.read(buffer);
                while (bytesRead >= 0) {
                    rOut.write(buffer, 0, bytesRead);
                    bytesRead = is.read(buffer);
                }

                rOut.flush();
                rOut.close();
            }
            is.close();
            return tmpRFile;
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
    }
    private static volatile boolean exiting = false;

    private static class RStreamReader implements Runnable {

        private final InputStream err;

        RStreamReader(InputStream in) {
            err = in;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(err))) {
                String line;
                while (!exiting && ((line = in.readLine()) != null)) {
                    if (!line.trim().isEmpty()) {
                        Logger.getLogger(Rserve.class.getName()).log(Level.INFO, "R: {0}", line);
                    }
                }
            } catch (IOException ex) {
                if (!exiting) {
                    Logger.getLogger(Rserve.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
