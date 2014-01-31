package de.cebitec.mgx.gpms.impl;

import com.jolbox.bonecp.BoneCPDataSource;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.gpms.data.DBProjectI;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private EntityManagerFactory emf = null;
    private final DataSource ds;
    //private String jndiname;
    private String login = null;

    public GPMSMaster(DBMembershipI m, DataSource ds) {
        membership = m;
        this.ds = ds;
        lastUsed = System.currentTimeMillis();
    }
    
    public void setEntityManagerFactory(EntityManagerFactory ef) {
        emf = ef;
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        lastUsed = System.currentTimeMillis();
        return emf;
    }

    public void close() {
        ((BoneCPDataSource)ds).close();
        //ds.shutdown();
        
//        // unpublish the datasource
//        Context ctx;
//        try {
//            ctx = new InitialContext();
//            BoneCPDataSource bds = (BoneCPDataSource) ctx.lookup(jndiname);
//            if (bds != null) {
//                bds.close();
//            }
//            log("Closing and unbinding JNDI " + jndiname);
//            ctx.unbind(jndiname);
//        } catch (NamingException ex) {
//            log(ex.getMessage());
//        }
    }

    @Override
    public DataSource getDataSource() {
        lastUsed = System.currentTimeMillis();
        return ds;
    }

    public void lastObtained(long curTime) {
        lastUsed = curTime;
    }

    public long lastObtained() {
        return lastUsed;
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

    @Override
    public void log(String msg) {
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
