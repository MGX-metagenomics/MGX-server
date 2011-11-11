package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.mgx.gpms.impl.data.ProjectClass;
import de.cebitec.mgx.gpms.impl.data.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "GPMS")
@Startup
public class GPMS implements DBGPMSI {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMSConfiguration")
    private GPMSConfiguration config;
    //
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms/GPMSSessions")
    private GPMSSessions sessions;
    //
    @Resource
    private EJBContext ejbCtx;
    //
    private ThreadLocal<DBMasterI> currentMaster = new ThreadLocal<DBMasterI>();
    //
    private static Set<ProjectClassI> supportedPClasses = new HashSet<ProjectClassI>();
    //
    private final static Logger logger = Logger.getLogger(GPMS.class.getPackage().getName());
    //
    @Resource(mappedName = "jdbc/GPMS")
    private DataSource gpmsds;

    public String getGPMSConfigDirectory() {
        return config.getGPMSConfigDirectory();
    }

    public Connection getConnection() {
        Connection c = null;
        try {
            c = gpmsds.getConnection();
        } catch (SQLException ex) {
            log(ex.getMessage());
        }
        return c;
    }

    @Override
    public void createMaster(DBMembershipI m) {
        DBMasterI master = sessions.getMaster(m);
        master.setLogin(getCurrentUser().getLogin());
        this.currentMaster.set(master);
    }

    @Override
    public DBMasterI getCurrentMaster() {
        return currentMaster.get();
    }

    @Override
    public UserI getCurrentUser() {
        String login = ejbCtx.getCallerPrincipal().getName();
        return new User(this, login);
    }

    @Override
    public void registerEMFResolver(EMFNameResolver enr) {
        EMFNameResolver.registerResolver(enr);
    }

    @Override
    public void unregisterEMFResolver(EMFNameResolver enr) {
        EMFNameResolver.unregisterResolver(enr);
    }

    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void cleanupMembershipCache(Timer timer) {
        User.clearMembershipCache();
    }

    public void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    public void log(String msg, Object... args) {
        logger.log(Level.INFO, String.format(msg, args));
    }

    @Override
    public Set<ProjectClassI> getProjectClasses() {
        // FIXME - return all available GPMS project classes
        Set<ProjectClassI> ret = new HashSet<ProjectClassI>();
        ret.add(new ProjectClass(this, "MGX"));
        return ret;
    }

    @Override
    public Set<ProjectClassI> getSupportedProjectClasses() {
        return supportedPClasses;
    }

    @Override
    public void registerProjectClass(String pc) {
        ProjectClassI projectClass = new ProjectClass(this, pc);
        if (!supportedPClasses.contains(projectClass)) {
            supportedPClasses.add(projectClass);
        }
    }

    @Override
    public void unregisterProjectClass(String pc) {
        ProjectClassI projectClass = new ProjectClass(this, pc);
        supportedPClasses.remove(projectClass);
    }
}
