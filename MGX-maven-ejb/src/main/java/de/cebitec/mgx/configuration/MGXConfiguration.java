package de.cebitec.mgx.configuration;

import de.cebitec.mgx.dispatcher.common.DispatcherConfigBase;
import de.cebitec.mgx.dispatcher.common.MGXDispatcherException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "MGXConfiguration")
@Startup
public class MGXConfiguration extends DispatcherConfigBase {

    private final Properties config = new Properties();

    public MGXConfiguration() {
    }

    @PostConstruct
    public void create() {
        String cfgFile = new StringBuilder(System.getProperty("user.dir"))
                .append(File.separator)
                .append("mgx_server.properties")
                .toString();

        File f = new File(cfgFile);
        if (!f.exists()) {
            throw new RuntimeException("MGX configuration failed, " + cfgFile.toString() + " missing");
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(cfgFile);
            config.load(in);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(MGXConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        Logger.getLogger(MGXConfiguration.class.getName()).log(Level.INFO, "MGX server configuration done.");
    }

    /*
     * settings for the MGX global zone
     */
//    public String getMGXGlobalUser() {
//        return config.getProperty("mgxglobal_user");
//    }
//
//    public String getMGXGlobalPassword() {
//        return config.getProperty("mgxglobal_pass");
//    }
//
//    public String getMGXGlobalJDBCURL() {
//        return config.getProperty("mgxglobal_jdbc_url");
//    }
    public String getMGXGlobalStorageDir() {
        return config.getProperty("mgxglobal_persistent_dir");
    }

    /*
     * settings specific to project database
     */
    public int getTransferTimeout() {
        return getInt("mgx_transfer_timeout");
    }

    public int getSQLBulkInsertSize() {
        return getInt("sql_bulk_insert_size");
    }

    public String getPersistentDirectory() {
        return config.getProperty("mgx_persistent_dir");
    }

    public String getMGXUser() {
        return config.getProperty("mgx_user");
    }

    public String getMGXPassword() {
        return config.getProperty("mgx_password");
    }

//    public String getValidatorExecutable() {
//        return config.getProperty("mgx_graphvalidate");
//    }
    public File getPluginDump() {
        StringBuilder sb = new StringBuilder(getMGXGlobalStorageDir());
        if (!getMGXGlobalStorageDir().endsWith(File.separator)) {
            sb.append(File.separatorChar);
        }
        sb.append("plugins.xml");

        File ret = new File(sb.toString());
        assert ret.exists();

        return ret;
    }

    public String getDispatcherHost() throws MGXDispatcherException {

        /*
         * dispatcher host might be changing, therefore we have to read
         * this file every time
         */
        File f = new File(dispatcherHostFile);
        if (!f.exists()) {
            throw new MGXDispatcherException("Dispatcher host file missing, dispatcher not running?");
        }

        Properties p = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } catch (IOException ex) {
            throw new MGXDispatcherException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MGXConfiguration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return p.getProperty("mgx_dispatcherhost");
    }

    public String getDispatcherToken() throws MGXDispatcherException {

        /*
         * dispatcher host might be changing, therefore we have to read
         * this file every time
         */
        File f = new File(dispatcherHostFile);
        if (!f.exists()) {
            throw new MGXDispatcherException("Dispatcher host file missing, dispatcher not running?");
        }

        Properties p = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } catch (IOException ex) {
            throw new MGXDispatcherException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MGXConfiguration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return p.getProperty("mgx_dispatchertoken");
    }

    /*
     * internal conversion
     */
    private int getInt(String key) {
        return Integer.parseInt(config.getProperty(key));
    }
}
