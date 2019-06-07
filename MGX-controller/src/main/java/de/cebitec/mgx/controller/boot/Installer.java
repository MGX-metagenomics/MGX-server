package de.cebitec.mgx.controller.boot;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.util.GPMSDataSourceSelector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class Installer {

    @EJB
    private DBGPMSI gpms;
    //private final EMFNameResolver resolver = new MGXPUResolver();
    private final MGXDataSourceSelector dsSelector = new MGXDataSourceSelector();
    private final MGX2DataSourceSelector dsSelector2 = new MGX2DataSourceSelector();
    //
    private static final Logger LOG = Logger.getLogger(Installer.class.getName());

    @PostConstruct
    public void start() {
        LOG.log(Level.INFO, "Starting MGX: {0}", gpms);
        GPMSDataSourceSelector.registerSelector("MGX", dsSelector);
        GPMSDataSourceSelector.registerSelector("MGX-2", dsSelector2);
    }

    @PreDestroy
    public void stop() {
        LOG.log(Level.INFO, "Exiting MGX: {0}", gpms);
        GPMSDataSourceSelector.unregisterSelector("MGX", dsSelector);
        GPMSDataSourceSelector.unregisterSelector("MGX-2", dsSelector2);
    }
}
