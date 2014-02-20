//package de.cebitec.mgx.statistics;
//
//import de.cebitec.mgx.controller.MGXException;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import javax.ejb.Singleton;
//import javax.ejb.Startup;
//import org.rosuda.JRI.Rengine;
//
///**
// *
// * @author sjaenick
// */
//@Singleton(mappedName = "R")
//@Startup
//public class R {
//
//    private Rengine re = null;
//
//    public Rengine getR() throws MGXException {
//        re = Rengine.getMainEngine();
//        if (re == null) {
//            re = new Rengine(new String[]{("--vanilla"), ("--silent")}, false, new RLogger());
//            if (!re.waitForR()) {
//                throw new MGXException("Error creating R instance");
//            }
//        }
//        re.assign("tmpdir", System.getProperty("java.io.tmpdir"));
//        File scriptFile = createScript();
//
//        if (!scriptFile.exists()) {
//            throw new MGXException("Source file " + scriptFile.getAbsolutePath() + " missing.");
//        }
//        re.eval("source(\"" + scriptFile.getAbsolutePath() + "\")");
//
//        return re;
//    }
//
//    @PostConstruct
//    public void start() {
//        if (!Rengine.versionCheck()) {
//            throw new RuntimeException("** R Version mismatch - Java files don't match library version.");
//        }
//    }
//
//    @PreDestroy
//    public void stop() {
//        if (re != null) {
//            re.end();
//            re = null;
//        }
//    }
//
//    private File createScript() throws MGXException {
//        try (InputStream is = getClass().getClassLoader().getResourceAsStream("de/cebitec/mgx/statistics/Rfunctions.r")) {
//            StringBuffer tmpRFileName = new StringBuffer('.').append("RSource").append(".R");
//            File tmpRFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), tmpRFileName.toString());
//
//            FileOutputStream rOut = new FileOutputStream(tmpRFile);
//
//            byte[] buffer = new byte[1024];
//
//            int bytesRead = is.read(buffer);
//            while (bytesRead >= 0) {
//                rOut.write(buffer, 0, bytesRead);
//                bytesRead = is.read(buffer);
//            }
//
//            rOut.flush();
//            rOut.close();
//            return tmpRFile;
//        } catch (IOException ex) {
//            throw new MGXException(ex.getMessage());
//        }
//    }
//}
