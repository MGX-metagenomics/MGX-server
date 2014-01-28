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

    public static HikariDataSource createDataSource(DBMembershipI m) {

        String poolname = new StringBuilder("DS-")
                .append(m.getProject().getName())
                .append("-")
                .append(m.getRole().getName())
                .toString();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName(poolname);
        cfg.setMinimumPoolSize(5);
        cfg.setMaximumPoolSize(5);
        cfg.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        cfg.addDataSourceProperty("user", m.getRole().getDBUser());
        cfg.addDataSourceProperty("password", m.getRole().getDBPassword());
        cfg.addDataSourceProperty("serverName", m.getProject().getDBConfig().getDatabaseHost());
        cfg.addDataSourceProperty("portNumber", m.getProject().getDBConfig().getDatabasePort());
        cfg.addDataSourceProperty("databaseName", m.getProject().getDBConfig().getDatabaseName());
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
}
