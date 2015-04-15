package de.cebitec.mgx.gpms.impl;

import com.zaxxer.hikari.HikariDataSource;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.gpms.data.DBProjectI;
import java.util.Objects;
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
        ((HikariDataSource)ds).close();
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.membership);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GPMSMaster other = (GPMSMaster) obj;
        if (!Objects.equals(this.membership, other.membership)) {
            return false;
        }
        return true;
    }
    
    
}
