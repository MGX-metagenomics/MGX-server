package de.cebitec.mgx.controller;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.util.EMFNameResolver;
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

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMS")
    private DBGPMSI gpms;
    private final EMFNameResolver resolver = new MGXPUResolver();
    //
    private static final Logger LOG = Logger.getLogger(Installer.class.getName());
    

    @PostConstruct
    public void start() {
        LOG.log(Level.INFO, "Starting MGX: {0}", gpms);
        gpms.registerEMFResolver(resolver);
        gpms.registerProjectClass("MGX");
    }

    @PreDestroy
    public void stop() {
        LOG.log(Level.INFO, "Exiting MGX: {0}", gpms);
        gpms.unregisterProjectClass("MGX");
        gpms.unregisterEMFResolver(resolver);
    }
}
