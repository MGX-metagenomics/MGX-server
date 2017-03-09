package de.cebitec.mgx.statistics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

/**
 *
 * @author sjaenick
 */
public class RWrappedConnection implements AutoCloseable {

    private final RConnection conn;

    public RWrappedConnection(RConnection conn) throws RserveException {
        this.conn = conn;
    }

//    @Override
//    public void finalize() {
//        conn.finalize();
//        super.finalize();
//    }

    public int getServerVersion() {
        return conn.getServerVersion();
    }

    @Override
    public void close() {
        if (conn != null) {
            conn.close();
        }
//        return ret;
    }

    public void voidEval(String string) throws RserveException {
        conn.voidEval(string);
    }

    public RSession voidEvalDetach(String string) throws RserveException {
        return conn.voidEvalDetach(string);
    }

    public REXP eval(String string) throws RserveException {
        conn.assign(".tmp.", string);
        REXP r = null;
        try {
            r = conn.parseAndEval("try(eval(parse(text=.tmp.)),silent=TRUE)");
            if (r.inherits("try-error")) {
                System.err.println("Expression: " + string);
                System.err.println("Error: " + r.asString());
            }
        } catch (REngineException | REXPMismatchException ex) {
            Logger.getLogger(RWrappedConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }

    public void assign(String string, String string1) throws RserveException {
        conn.assign(string, string1);
    }

    public void assign(String string, REXP rexp) throws RserveException {
        conn.assign(string, rexp);
    }
    
    

    public RFileInputStream openFile(String string) throws IOException {
        return conn.openFile(string);
    }

    public RFileOutputStream createFile(String string) throws IOException {
        return conn.createFile(string);
    }

    public void removeFile(String string) throws RserveException {
        conn.removeFile(string);
    }

    public void shutdown() throws RserveException {
        conn.shutdown();
    }

    public void setSendBufferSize(long l) throws RserveException {
        conn.setSendBufferSize(l);
    }

    public void setStringEncoding(String string) throws RserveException {
        conn.setStringEncoding(string);
    }

    public void login(String string, String string1) throws RserveException {
        conn.login(string, string1);
    }

    public RSession detach() throws RserveException {
        return conn.detach();
    }

    public boolean isConnected() {
        return conn.isConnected();
    }

    public boolean needLogin() {
        return conn.needLogin();
    }

    public String getLastError() {
        return conn.getLastError();
    }

    public void serverEval(String string) throws RserveException {
        conn.serverEval(string);
    }

    public void serverSource(String string) throws RserveException {
        conn.serverSource(string);
    }

    public void serverShutdown() throws RserveException {
        conn.serverShutdown();
    }

    public REXP parse(String string, boolean bln) throws REngineException {
        return conn.parse(string, bln);
    }

    public REXP eval(REXP rexp, REXP rexp1, boolean bln) throws REngineException {
        return conn.eval(rexp, rexp1, bln);
    }

    public REXP parseAndEval(String string, REXP rexp, boolean bln) throws REngineException {
        return conn.parseAndEval(string, rexp, bln);
    }

    public void assign(String string, REXP rexp, REXP rexp1) throws REngineException {
        conn.assign(string, rexp, rexp1);
    }

    public REXP get(String string, REXP rexp, boolean bln) throws REngineException {
        return conn.get(string, rexp, bln);
    }

    public REXP resolveReference(REXP rexp) throws REngineException {
        return conn.resolveReference(rexp);
    }

    public REXP createReference(REXP rexp) throws REngineException {
        return conn.createReference(rexp);
    }

    public void finalizeReference(REXP rexp) throws REngineException {
        conn.finalizeReference(rexp);
    }

    public REXP getParentEnvironment(REXP rexp, boolean bln) throws REngineException {
        return conn.getParentEnvironment(rexp, bln);
    }

    public REXP newEnvironment(REXP rexp, boolean bln) throws REngineException {
        return conn.newEnvironment(rexp, bln);
    }

    public void assign(String varname, double[] data) throws REngineException {
        conn.assign(varname, data);
    }

    void assign(String tmp, String[] colAliases) throws REngineException {
        conn.assign(tmp, colAliases);
    }
    
    

}
