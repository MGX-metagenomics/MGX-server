package de.cebitec.mgx.global;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "MGXGlobal")
@Startup
public class MGXGlobal {

    @Resource(mappedName = "jdbc/MGXGlobal")
    private DataSource globalDS;

    private final static Logger logger = Logger.getLogger(MGXGlobal.class.getName());

    private final GlobalToolDAO tooldao;
    private final GlobalTermDAO termdao;
    private final GlobalReferenceDAO refdao;
    private final GlobalRegionDAO regiondao;

    public MGXGlobal() {
        tooldao = new GlobalToolDAO(this);
        termdao = new GlobalTermDAO(this);
        refdao = new GlobalReferenceDAO(this);
        regiondao = new GlobalRegionDAO(this);
    }

    @PostConstruct
    public void init() {
        if (globalDS == null) {
            try {
                Context ctx = new InitialContext();
                globalDS = (DataSource) ctx.lookup("jdbc/MGXGlobal");
            } catch (NamingException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    Connection getConnection() throws SQLException {
        return globalDS.getConnection();
    }

    public GlobalToolDAO getToolDAO() {
        return tooldao;
    }

    public GlobalTermDAO getTermDAO() {
        return termdao;
    }

    public GlobalReferenceDAO getReferenceDAO() {
        return refdao;
    }

    public GlobalRegionDAO getRegionDAO() {
        return regiondao;
    }

    public void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
