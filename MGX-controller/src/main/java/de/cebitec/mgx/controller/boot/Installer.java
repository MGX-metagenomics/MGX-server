package de.cebitec.mgx.controller.boot;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.util.GPMSDataSourceSelector;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class Installer {

    @EJB
    private DBGPMSI gpms;

    private static final Logger LOG = Logger.getLogger(Installer.class.getName());

    @PostConstruct
    public void start() {
        LOG.log(Level.INFO, "Starting MGX: {0}", gpms);
        GPMSDataSourceSelector.registerSelector("MGX-2", new MGX2DataSourceSelector());
    }

    @PreDestroy
    public void stop() {
        LOG.log(Level.INFO, "Exiting MGX: {0}", gpms);
        GPMSDataSourceSelector.unregisterSelector("MGX-2");
    }
}
