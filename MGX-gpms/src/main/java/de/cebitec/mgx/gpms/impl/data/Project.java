package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.data.DBConfigI;
import de.cebitec.gpms.data.DBProjectI;
import de.cebitec.mgx.gpms.GPMSException;
import de.cebitec.mgx.gpms.impl.GPMS;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author sjaenick
 */
public class Project implements DBProjectI {

    private final String name;
    private final GPMS gpms;
    private final ProjectClassI pclass;
    private DBConfigI dbcfg;
    private String jdbcUrl;
    private String host;
    private String dbname;
    //
    private final static String sql = new StringBuffer("SELECT Host.hostname as host, DataSource.name as dbname, ")
            .append("CONCAT('jdbc:', LOWER(DBMS_Type.name), '://', Host.hostname, ':',")
            .append("Host.port, '/', DataSource.name) as jdbc ")
            .append("from Project left join Project_datasources on (Project._id = Project_datasources._parent_id)")
            .append("      left join DataSource on (Project_datasources._array_value = DataSource._id)")
            .append("      left join DataSource_Type on (DataSource.datasource_type_id = DataSource_Type._id)")
            .append("      left join DataSource_DB on (DataSource._id = DataSource_DB._parent_id)")
            .append("      left join Host on (DataSource_DB.host_id = Host._id)")
            .append("      left join DBMS_Type on (DataSource_DB.dbms_type_id = DBMS_Type._id) ")
            .append("WHERE Project.name=?")
            .toString();

    public Project(GPMS gpms, String name, ProjectClassI pclass) {
        this.gpms = gpms;
        this.name = name;
        this.pclass = pclass;
        dbcfg = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProjectClassI getProjectClass() {
        return pclass;
    }

    public String getJDBCUrl() throws GPMSException {
        if (dbcfg == null) {
            load();
        }
        return dbcfg.getURI();
    }

    public String getDBType() throws GPMSException {
        return getJDBCUrl().split(":")[1];
    }

    public String getDatabaseHost() throws GPMSException {
        if (dbcfg == null) {
            load();
        }
        return host;
    }

    public String getDatabaseName() throws GPMSException {
        if (dbcfg == null) {
            load();
        }
        return dbname;
    }
    @Override
    public DBConfigI getDBConfig() {
        if (dbcfg == null) {
            try {
                load();
            } catch (GPMSException ex) {
            }
        }

        return new DBConfigI() {

            @Override
            public String getURI() {
                return jdbcUrl;
            }

            @Override
            public String getDatabaseHost() {
                return host;
            }

            @Override
            public String getDatabaseName() {
                return dbname;
            }
        };
    }

    private void load() throws GPMSException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet res = null;

        try {
            conn = gpms.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, this.getName());
            res = stmt.executeQuery();
            if (res.next()) {
                host = res.getString(1);
                dbname = res.getString(2);
                jdbcUrl = res.getString(3);
            }
        } catch (SQLException e) {
            throw new GPMSException(e.getMessage());
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                throw new GPMSException(ex.getMessage());
            }
        }

    }
}
