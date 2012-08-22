package de.cebitec.mgx.gpms.util;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import de.cebitec.gpms.data.DBMembershipI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class DataSourceFactory {

    private final static Logger logger = Logger.getLogger(DataSourceFactory.class.getPackage().getName());

//    public static DataSource getDataSource(DBMembershipI mbr) {
//
//        // lookup final datasource for project or create a new one
//        String jndiname = new StringBuilder("jdbc/")
//                .append(mbr.getProject().getProjectClass().getName())
//                .append("/").append(mbr.getProject().getName())
//                .toString();
//
//        DataSource ds = null;
//
//        try {
//            ds = InitialContext.<DataSource>doLookup(jndiname);
//        } catch (NamingException ex) {
//        }
//
//        if (ds == null) {
//            log("Creating datasource for JNDI name " + jndiname);
//            try {
//                ds = createDataSource(mbr);
//                // publish the datasource
//                Context ctx = new InitialContext();
//                ctx.bind(jndiname, ds);
//            } catch (NamingException | GPMSException ex) {
//                log(ex.getMessage());
//            }
//        } else {
//            log("Re-using old datasource from JNDI " + jndiname);
//        }
//
//        return ds;
//    }

    public static DataSource createDataSource(DBMembershipI m) {

        String jdbc = m.getProject().getDBConfig().getURI();

        String poolname = new StringBuilder("DS-")
                .append(m.getProject().getName())
                .append(m.getRole().getName())
                .toString();

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

        return new BoneCPDataSource(cfg);
    }

    private static void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
