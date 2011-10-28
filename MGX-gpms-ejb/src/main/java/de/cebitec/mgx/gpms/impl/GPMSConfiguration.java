package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.GPMSException;
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
public class GPMSConfiguration {

    protected Properties config = null;

//    @Override
//    public String getGPMSUser() {
//        return config.getProperty("gpms_user");
//    }
//
//    @Override
//    public String getGPMSPassword() {
//        return config.getProperty("gpms_password");
//    }
//
//    @Override
//    public String getGPMSURL() {
//        return config.getProperty("gpms_jdbc_url");
//    }
//
//    @Override
//    public String getGPMSDriverClass() {
//        return config.getProperty("gpms_driverclass");
//    }

    public String getGPMSConfigDirectory() {
        return config.getProperty("gpms_configdir");
    }

    @PostConstruct
    public void startup() throws GPMSException {

        String cfgFile = new StringBuilder(System.getProperty("user.dir"))
                .append(File.separator)
                .append("gpms.properties")
                .toString();

        File f = new File(cfgFile);
        if (!f.exists()) {
            throw new GPMSException("GPMS configuration failed: " + cfgFile + " missing");
        }

        config = new Properties();
        FileInputStream in = null;

        try {
            in = new FileInputStream(cfgFile);
            config.load(in);
            in.close();
        } catch (Exception ex) {
            throw new GPMSException(ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(GPMSConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
