package de.cebitec.mgx.controller;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.global.MGXGlobal;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

/**
 *
 * @author sjaenick
 */
public class ControllerFactory {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMS")
    DBGPMSI gpms;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXGlobal")
    MGXGlobal global;

    @Produces
    @MGX
    @RequestScoped
    MGXController getController() {
        DBMasterI currentMaster = gpms.getCurrentMaster();
        return new MGXControllerImpl(currentMaster, global, mgxconfig);
    }

    public void dispose(@Disposes MGXController c) {
        c.close();
    }
}
