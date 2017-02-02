package de.cebitec.mgx.configuration;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
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
@Singleton
@Startup
public class MGXConfiguration implements MGXConfigurationI {

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
            throw new RuntimeException("MGX configuration failed, " + cfgFile + " missing");
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
        Logger.getLogger(getClass().getName()).log(Level.INFO, "MGX server configuration done.");
    }

    /*
     * settings for the MGX global zone
     */
    @Override
    public String getMGXGlobalStorageDir() {
        String ret = config.getProperty("mgxglobal_persistent_dir");
        if (!ret.endsWith(File.separator)) {
            ret = ret + File.separator;
        }
        return ret;
    }

    /*
     * settings specific to project database
     */
    @Override
    public int getTransferTimeout() {
        return getInt("mgx_transfer_timeout");
    }

    @Override
    public int getSQLBulkInsertSize() {
        return getInt("sql_bulk_insert_size");
    }

    @Override
    public File getPersistentDirectory() {
        return new File(config.getProperty("mgx_persistent_dir"));
    }

    @Override
    public String getMGXUser() {
        return config.getProperty("mgx_user");
    }

    @Override
    public String getMGXPassword() {
        return config.getProperty("mgx_password");
    }

    @Override
    public File getPluginDump() {
        File ret = new File(getMGXGlobalStorageDir() + "plugins.xml");

        if (!ret.exists()) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, "Plugin dump file missing at expected location ({0}).", ret.getAbsolutePath());
            return null;
        }

        return ret;
    }

//    public String getDispatcherHost() throws MGXDispatcherException {
//
//        /*
//         * dispatcher host might be changing, therefore we have to read
//         * this file every time
//         */
//        File f = new File(dispatcherHostFile);
//        if (!f.exists()) {
//            throw new MGXDispatcherException("Dispatcher host file missing, dispatcher not running?");
//        }
//
//        Properties p = new Properties();
//        FileInputStream in = null;
//        try {
//            in = new FileInputStream(f);
//            p.load(in);
//        } catch (IOException ex) {
//            throw new MGXDispatcherException(ex);
//        } finally {
//            try {
//                if (in != null) {
//                    in.close();
//                }
//            } catch (IOException ex) {
//                Logger.getLogger(MGXConfiguration.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        return p.getProperty("mgx_dispatcherhost");
//    }
//    public String getDispatcherToken() throws MGXDispatcherException {
//
//        /*
//         * dispatcher host might be changing, therefore we have to read
//         * this file every time
//         */
//        File f = new File(dispatcherHostFile);
//        if (!f.exists()) {
//            throw new MGXDispatcherException("Dispatcher host file missing, dispatcher not running?");
//        }
//
//        Properties p = new Properties();
//        FileInputStream in = null;
//        try {
//            in = new FileInputStream(f);
//            p.load(in);
//        } catch (IOException ex) {
//            throw new MGXDispatcherException(ex);
//        } finally {
//            try {
//                if (in != null) {
//                    in.close();
//                }
//            } catch (IOException ex) {
//                Logger.getLogger(MGXConfiguration.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        return p.getProperty("mgx_dispatchertoken");
//    }

    /*
     * internal conversion
     */
    private int getInt(String key) {
        return Integer.parseInt(config.getProperty(key));
    }
}
