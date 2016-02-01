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
public class RWrappedConnection extends RConnection {

    private final RConnection conn;

    public RWrappedConnection(RConnection conn) throws RserveException {
        this.conn = conn;
    }

    @Override
    public void finalize() {
        conn.finalize();
        super.finalize();
    }

    @Override
    public int getServerVersion() {
        return conn.getServerVersion();
    }

    @Override
    public boolean close() {
        boolean ret = true;
        if (conn != null) {
            ret = conn.close();
        }
        return ret;
    }

    @Override
    public void voidEval(String string) throws RserveException {
        conn.voidEval(string);
    }

    @Override
    public RSession voidEvalDetach(String string) throws RserveException {
        return conn.voidEvalDetach(string);
    }

    @Override
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

    @Override
    public void assign(String string, String string1) throws RserveException {
        conn.assign(string, string1);
    }

    @Override
    public void assign(String string, REXP rexp) throws RserveException {
        conn.assign(string, rexp);
    }

    @Override
    public RFileInputStream openFile(String string) throws IOException {
        return conn.openFile(string);
    }

    @Override
    public RFileOutputStream createFile(String string) throws IOException {
        return conn.createFile(string);
    }

    @Override
    public void removeFile(String string) throws RserveException {
        conn.removeFile(string);
    }

    @Override
    public void shutdown() throws RserveException {
        conn.shutdown();
    }

    @Override
    public void setSendBufferSize(long l) throws RserveException {
        conn.setSendBufferSize(l);
    }

    @Override
    public void setStringEncoding(String string) throws RserveException {
        conn.setStringEncoding(string);
    }

    @Override
    public void login(String string, String string1) throws RserveException {
        conn.login(string, string1);
    }

    @Override
    public RSession detach() throws RserveException {
        return conn.detach();
    }

    @Override
    public boolean isConnected() {
        return conn.isConnected();
    }

    @Override
    public boolean needLogin() {
        return conn.needLogin();
    }

    @Override
    public String getLastError() {
        return conn.getLastError();
    }

    @Override
    public void serverEval(String string) throws RserveException {
        conn.serverEval(string);
    }

    @Override
    public void serverSource(String string) throws RserveException {
        conn.serverSource(string);
    }

    @Override
    public void serverShutdown() throws RserveException {
        conn.serverShutdown();
    }

    @Override
    public REXP parse(String string, boolean bln) throws REngineException {
        return conn.parse(string, bln);
    }

    @Override
    public REXP eval(REXP rexp, REXP rexp1, boolean bln) throws REngineException {
        return conn.eval(rexp, rexp1, bln);
    }

    @Override
    public REXP parseAndEval(String string, REXP rexp, boolean bln) throws REngineException {
        return conn.parseAndEval(string, rexp, bln);
    }

    @Override
    public void assign(String string, REXP rexp, REXP rexp1) throws REngineException {
        conn.assign(string, rexp, rexp1);
    }

    @Override
    public REXP get(String string, REXP rexp, boolean bln) throws REngineException {
        return conn.get(string, rexp, bln);
    }

    @Override
    public REXP resolveReference(REXP rexp) throws REngineException {
        return conn.resolveReference(rexp);
    }

    @Override
    public REXP createReference(REXP rexp) throws REngineException {
        return conn.createReference(rexp);
    }

    @Override
    public void finalizeReference(REXP rexp) throws REngineException {
        conn.finalizeReference(rexp);
    }

    @Override
    public REXP getParentEnvironment(REXP rexp, boolean bln) throws REngineException {
        return conn.getParentEnvironment(rexp, bln);
    }

    @Override
    public REXP newEnvironment(REXP rexp, boolean bln) throws REngineException {
        return conn.newEnvironment(rexp, bln);
    }

}
