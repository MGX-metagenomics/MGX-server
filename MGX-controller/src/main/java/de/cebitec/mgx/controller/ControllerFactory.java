package de.cebitec.mgx.controller;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

/**
 *
 * @author sjaenick
 */
public class ControllerFactory {

    @EJB
    DBGPMSI gpms;
    @EJB
    MGXConfigurationI mgxconfig;

    @Produces
    @MGX
    @RequestScoped
    MGXController getController() {
        return new MGXControllerImpl(gpms.<JPAMasterI>getCurrentMaster(), mgxconfig);
    }

    public void dispose(@Disposes MGXController c) {
        try {
            c.close();
        } catch (Exception ex) {
            Logger.getLogger(ControllerFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
