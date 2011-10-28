package de.cebitec.mgx.gpms.impl.data;


import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.GPMSException;
import de.cebitec.gpms.data.ProjectClassI;
import de.cebitec.gpms.data.ProjectI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author sjaenick
 */
public class Project implements ProjectI {

    private String name;
    private GPMS gpms;
    private ProjectClassI pclass;
    private String jdbcUrl;
    private String host;
    private String dbname;
    private boolean loaded = false;

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
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProjectClassI getProjectClass() {
        return pclass;
    }

    @Override
    public String getJDBCUrl() throws GPMSException {
        if (!loaded)
            load();
        return jdbcUrl;
    }

    @Override
    public String getDBType() throws GPMSException {
        return getJDBCUrl().split(":")[1];
    }

    @Override
    public String getDatabaseHost() throws GPMSException {
        if (!loaded)
            load();
        return host;
    }

    @Override
    public String getDatabaseName() throws GPMSException {
        if (!loaded)
            load();
        return dbname;
    }

    private void load() throws GPMSException {

        System.err.println("SQL: loadProject()");

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

        loaded = true;
    }
}
