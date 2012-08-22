package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.gpms.data.ProxyDataSourceI;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.mgx.gpms.impl.data.ProjectClass;
import de.cebitec.mgx.gpms.impl.data.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
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
    private ProxyDataSourceI proxyDS;
    private EntityManagerFactory emf = null;
    //
    @Resource
    private EJBContext ejbCtx;
    //
    private ThreadLocal<GPMSMaster> currentMaster = new ThreadLocal<>();
    //
    private static Set<ProjectClassI> supportedPClasses = new HashSet<>();
    //
    private final static Logger logger = Logger.getLogger(GPMS.class.getPackage().getName());
    //
    @Resource(mappedName = "jdbc/GPMS")
    private DataSource gpmsds;

    @PostConstruct
    public void start() {
        proxyDS = new GPMSProxyDataSource(this);
        try {
            Context ctx = new InitialContext();
            ctx.bind(ProxyDataSourceI.JNDI_NAME, proxyDS);
        } catch (NamingException ex) {
            Logger.getLogger(GPMS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            Context ctx = new InitialContext();
            ctx.unbind(ProxyDataSourceI.JNDI_NAME);
        } catch (NamingException ex) {
            Logger.getLogger(GPMS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
    public void createMaster(DBMembershipI mbr) {
        GPMSMaster master = sessions.getMaster(mbr);
        master.setLogin(getCurrentUser().getLogin());
        currentMaster.set(master);

        if (emf == null) {
            emf = EMFNameResolver.createEMF(mbr, ProxyDataSourceI.JNDI_NAME, "MGX-PU");
        }
        master.setEntityManagerFactory(emf);
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

    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = false)
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
        Set<ProjectClassI> ret = new HashSet<>();
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
