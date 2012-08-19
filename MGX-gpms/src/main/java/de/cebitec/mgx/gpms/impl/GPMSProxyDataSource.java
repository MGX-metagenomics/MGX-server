
package de.cebitec.mgx.gpms.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
/**
 *
 * @author sj
 */
@Singleton
@Startup        
public class GPMSProxyDataSource implements DataSource {
    
    private DataSource currentDS = null;
    
    public void setCurrentDataSource(DataSource cur) {
        currentDS = cur;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return currentDS.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return currentDS.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return currentDS.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return currentDS.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return currentDS.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        currentDS.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        currentDS.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return currentDS.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return currentDS.getParentLogger();
    }
}
