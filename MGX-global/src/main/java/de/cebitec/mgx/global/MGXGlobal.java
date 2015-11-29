package de.cebitec.mgx.global;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "MGXGlobal")
@Startup
public class MGXGlobal {

    @Resource(mappedName = "jdbc/MGXGlobal")
    private DataSource ds;

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

    Connection getConnection() {
        try {
            return ds.getConnection();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
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
