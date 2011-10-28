package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.EMFNameResolver;
import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.GPMSMasterI;
import de.cebitec.gpms.data.MembershipI;
import de.cebitec.gpms.data.UserI;
import de.cebitec.mgx.gpms.impl.data.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
public class GPMSImpl implements GPMS {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms-ejb/GPMSConfiguration")
    private GPMSConfiguration config;
    //
    @EJB(lookup = "java:global/MGX-maven-ear/MGX-gpms-ejb/GPMSSessions")
    private GPMSSessions sessions;
    //
    @Resource
    private EJBContext ejbCtx;
    //
    private ThreadLocal<GPMSMasterI> currentMaster = new ThreadLocal<GPMSMasterI>();
    //
    private final static Logger logger = Logger.getLogger(GPMSImpl.class.getPackage().getName());
    //
    @Resource(mappedName = "jdbc/GPMS")
    private DataSource gpmsds;
    private final static Map<String, String> DRIVERS = new HashMap<String, String>() {

        {
            put("mysql", "com.mysql.jdbc.Driver");
            put("postgresql", "org.postgresql.Driver");
        }
    };

    @Override
    public String getGPMSConfigDirectory() {
        return config.getGPMSConfigDirectory();
    }

    @Override
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
    public void createMaster(MembershipI m) {
        GPMSMasterI master = sessions.getMaster(m);
        master.setLogin(getCurrentUser().getLogin());
        this.currentMaster.set(master);
    }

    @Override
    public GPMSMasterI getCurrentMaster() {
        return currentMaster.get();
    }

//    @Override
//    public void setCurrentMaster(GPMSMasterI currentMaster) {
//        this.currentMaster.set(currentMaster);
//    }

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

    public static String getDriverClass(String dbType) {
        return DRIVERS.get(dbType);
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
}
