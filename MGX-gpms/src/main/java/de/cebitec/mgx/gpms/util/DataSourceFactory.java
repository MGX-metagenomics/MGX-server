package de.cebitec.mgx.gpms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.cebitec.gpms.data.DBMembershipI;

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
        //cfg.setMinimumPoolSize(5);
        cfg.setMaximumPoolSize(50);
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

}
