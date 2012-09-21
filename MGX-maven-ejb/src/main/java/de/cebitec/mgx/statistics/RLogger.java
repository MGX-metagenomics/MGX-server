package de.cebitec.mgx.statistics;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

/**
 *
 * @author sjaenick
 */
public class RLogger implements RMainLoopCallbacks {
    
    private final static Logger logger = Logger.getLogger(RLogger.class.getPackage().getName());

    @Override
    public void rWriteConsole(Rengine rngn, String string, int i) {
        logger.log(Level.INFO, "R INFO: {0}", string);
    }

    @Override
    public void rBusy(Rengine rngn, int i) {
        logger.info("R BUSY");
    }

    @Override
    public String rReadConsole(Rengine rngn, String string, int i) {
        return "";
    }

    @Override
    public void rShowMessage(Rengine rngn, String string) {
        logger.log(Level.INFO, "R MESG: {0}", string);
    }

    @Override
    public String rChooseFile(Rengine rngn, int i) {
        return "";
    }

    @Override
    public void rFlushConsole(Rengine rngn) {
    }

    @Override
    public void rSaveHistory(Rengine rngn, String string) {
    }

    @Override
    public void rLoadHistory(Rengine rngn, String string) {
    }
    
}
