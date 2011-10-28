package de.cebitec.mgx.controller;


import de.cebitec.gpms.EMFNameResolver;
import de.cebitec.gpms.GPMS;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author ljelonek
 */
@Singleton
@Startup
public class Installer {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms-ejb/GPMSImpl")
    private GPMS gpms;
    private EMFNameResolver resolver = new MGXPUResolver();

    @PostConstruct
    public void start() {
        System.out.println("Starting MGX: " + gpms);
        gpms.registerEMFResolver(resolver);
    }

    @PreDestroy
    public void stop() {
        System.out.println("Exiting MGX: " + gpms);
        gpms.unregisterEMFResolver(resolver);
    }
}
