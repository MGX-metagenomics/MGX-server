package de.cebitec.mgx.gpms.impl;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.gpms.data.DBProjectI;
import de.cebitec.gpms.util.EMFNameResolver;
import de.cebitec.mgx.gpms.GPMSException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class GPMSMaster implements DBMasterI {

    private final static Logger logger = Logger.getLogger(GPMSMaster.class.getPackage().getName());
    private long lastUsed;
    private final DBMembershipI membership;
    private static EntityManagerFactory emf = null;
    private DataSource ds;
    private GPMSProxyDataSource proxyDS = null;
    private final String jndiname;
    private String login = null;
    private final String PUName;

    public GPMSMaster(DBMembershipI m, String PUName) {

        membership = m;
        this.PUName = PUName;

        jndiname = new StringBuilder("jdbc/")
                .append(m.getProject().getProjectClass().getName())
                .append("/").append(m.getProject().getName())
                .toString();
        
        
        try {
            ds = InitialContext.<DataSource>doLookup(jndiname);
            proxyDS = InitialContext.<GPMSProxyDataSource>doLookup("FIXME");
        } catch (NamingException ex) {
        }

        if (ds == null) {
            try {
                ds = createDataSource(membership);
                // publish the datasource
                Context ctx = new InitialContext();
                ctx.bind(jndiname, ds);
            } catch (NamingException | GPMSException ex) {
                log(ex.getMessage());
            }
        } else {
            log("Re-using old datasource from JNDI " + jndiname);
        }
        
        proxyDS.setCurrentDataSource(ds);

        lastUsed = System.currentTimeMillis();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        lastUsed = System.currentTimeMillis();
        if (emf == null) {
            //emf = EMFNameResolver.createEMF(membership, jndiname, PUName);
            emf = EMFNameResolver.createEMF(membership, "FIXME", PUName);
        }
        return emf;
    }

    public void close() {
//        if (emf != null) {
//            emf.close();
//        }

        // unpublish the datasource
        Context ctx;
        try {
            ctx = new InitialContext();
            BoneCPDataSource bds = (BoneCPDataSource) ctx.lookup(jndiname);
            if (bds != null) {
                bds.close();
            }
            log("Closing and unbinding JNDI " + jndiname);
            ctx.unbind(jndiname);
        } catch (NamingException ex) {
            log(ex.getMessage());
        }
    }

    @Override
    public DataSource getDataSource() {
        lastUsed = System.currentTimeMillis();
        return proxyDS;
    }

    public void lastObtained(long curTime) {
        lastUsed = curTime;
    }

    public long lastObtained() {
        return lastUsed;
    }

    private DataSource createDataSource(DBMembershipI m) throws GPMSException {

        String jdbc = m.getProject().getDBConfig().getURI();

        if (ds == null) {
            String poolname = new StringBuilder("DS-").append(membership.getProject().getName()).append(membership.getRole().getName()).toString();

            BoneCPConfig cfg = new BoneCPConfig();
            cfg.setLazyInit(true);
            cfg.setMaxConnectionsPerPartition(5);
            cfg.setMinConnectionsPerPartition(2);
            cfg.setPartitionCount(1);
            cfg.setJdbcUrl(jdbc);
            cfg.setUsername(m.getRole().getDBUser());
            cfg.setPassword(m.getRole().getDBPassword());
            cfg.setPoolName(poolname);
            cfg.setCloseConnectionWatch(false);
            cfg.setMaxConnectionAgeInSeconds(600);

            ds = new BoneCPDataSource(cfg);
        }
        return ds;
    }

    @Override
    public DBProjectI getProject() {
        lastUsed = System.currentTimeMillis();
        return membership.getProject();
    }

    @Override
    public RoleI getRole() {
        lastUsed = System.currentTimeMillis();
        return membership.getRole();
    }

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void setLogin(String name) {
        lastUsed = System.currentTimeMillis();
        this.login = name;
    }

    @Override
    public String getLogin() {
        lastUsed = System.currentTimeMillis();
        return login;
    }
}
