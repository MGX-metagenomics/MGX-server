package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.model.User;
import de.cebitec.gpms.util.GPMSDataLoaderI;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.*;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "GPMS")
@Startup
public class GPMS implements DBGPMSI {

    @EJB
    private GPMSSessions sessions;
    //
    @EJB
    private GPMSDataLoaderI loader;
    //
    @Resource
    private EJBContext ejbCtx;
    //
    private final static Logger logger = Logger.getLogger(GPMS.class.getPackage().getName());
    //

    @Override
    public Collection<ProjectClassI> getSupportedProjectClasses() {
        return loader.getSupportedProjectClasses();
    }

    @Override
    public <T extends MasterI> void createMaster(MembershipI mbr, Class<T> targetClass) {

        MasterI master = sessions.getMaster(mbr);

        // cache miss or different master class requested
        if (master == null || !(targetClass.isAssignableFrom(master.getClass()))) {
            long now = System.currentTimeMillis();
            try {
                master = loader.createMaster(mbr, targetClass);
            } catch (GPMSException ex) {
                Logger.getLogger(GPMS.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            sessions.registerMaster(mbr, master);
            now = System.currentTimeMillis() - now;
            log("Created new " + mbr.getProject().getName() + " master with role " + master.getRole().getName() + " for user " + getCurrentUser().getLogin() + " in " + now + "ms");
        }

        master.setUser(getCurrentUser());

        loader.setCurrentMaster(master);
    }

    @Override
    public <T extends MasterI> T getCurrentMaster() {
        return loader.getCurrentMaster();
    }

    @Override
    public UserI getCurrentUser() {
        String login = ejbCtx.getCallerPrincipal().getName();
        try {
            return new User(login, null, loader.getMemberships(login));
        } catch (GPMSException ex) {
            Logger.getLogger(GPMS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; //new User(login, null, Collections.EMPTY_LIST);
    }

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    private void log(String msg, Object... args) {
        logger.log(Level.INFO, String.format(msg, args));
    }

}
