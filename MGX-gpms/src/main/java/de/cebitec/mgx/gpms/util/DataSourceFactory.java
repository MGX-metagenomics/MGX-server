package de.cebitec.mgx.gpms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.cebitec.gpms.data.DBMembershipI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class DataSourceFactory {

    private final static Logger logger = Logger.getLogger(DataSourceFactory.class.getPackage().getName());

    public static HikariDataSource createDataSource(DBMembershipI m) {

        String jdbc = m.getProject().getDBConfig().getURI();

        String poolname = new StringBuilder("DS-")
                .append(m.getProject().getName())
                .append("-")
                .append(m.getRole().getName())
                .toString();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName(poolname);
        cfg.setMinimumPoolSize(5);
        cfg.setMaximumPoolSize(50);
        cfg.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        cfg.addDataSourceProperty("url", jdbc);
        cfg.setMaxLifetime(1000 * 60 * 2);  // 2 mins
        cfg.setIdleTimeout(1000 * 60 * 2);

        return new HikariDataSource(cfg);
    }

//    public static DataSource createDataSource(DBMembershipI m) {
//
//        String jdbc = m.getProject().getDBConfig().getURI();
//
//        String poolname = new StringBuilder("DS-")
//                .append(m.getProject().getName())
//                .append(m.getRole().getName())
//                .toString();
//
//        BoneCPConfig cfg = new BoneCPConfig();
//        cfg.setLazyInit(true);
//        cfg.setMaxConnectionsPerPartition(5);
//        cfg.setMinConnectionsPerPartition(2);
//        cfg.setPartitionCount(1);
//        cfg.setJdbcUrl(jdbc);
//        cfg.setUsername(m.getRole().getDBUser());
//        cfg.setPassword(m.getRole().getDBPassword());
//        cfg.setPoolName(poolname);
//        cfg.setCloseConnectionWatch(false);
//        cfg.setMaxConnectionAgeInSeconds(600);
//
//        return new BoneCPDataSource(cfg);
//    }
    private static void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
