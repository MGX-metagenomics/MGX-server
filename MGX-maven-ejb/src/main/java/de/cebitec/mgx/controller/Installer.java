package de.cebitec.mgx.controller;


import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.util.EMFNameResolver;
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
    private EMFNameResolver resolver = new MGXPUResolver();

    @PostConstruct
    public void start() {
        System.out.println("Starting MGX: " + gpms);
        gpms.registerEMFResolver(resolver);
        gpms.registerProjectClass("MGX");
    }

    @PreDestroy
    public void stop() {
        System.out.println("Exiting MGX: " + gpms);
        gpms.unregisterProjectClass("MGX");
        gpms.unregisterEMFResolver(resolver);
    }
}
