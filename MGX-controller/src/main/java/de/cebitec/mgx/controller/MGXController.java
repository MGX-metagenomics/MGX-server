package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.model.dao.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface MGXController extends AutoCloseable {

    public void log(Exception ex);

    public void log(String msg);

    public void log(String msg, Object... args);

//    public MGXConfigurationI getConfiguration();
    
    public File getPluginDump();

    public HabitatDAO getHabitatDAO();

    public AttributeTypeDAO getAttributeTypeDAO();

    public AttributeDAO getAttributeDAO();

    public SampleDAO getSampleDAO();

    public DNAExtractDAO getDNAExtractDAO();

    public SeqRunDAO getSeqRunDAO();

    public SequenceDAO getSequenceDAO();

    public ToolDAO getToolDAO();

    public JobDAO getJobDAO();

    public JobParameterDAO getJobParameterDAO();

    public ObservationDAO getObservationDAO();

    public ReferenceDAO getReferenceDAO();

    public MappingDAO getMappingDAO();

    public String getProjectName();

    public String getCurrentUser();

    public RoleI getCurrentRole();

//    public EntityManager getEntityManager();

    public DataSource getDataSource();

    public Connection getConnection() throws SQLException;

    public File getProjectDirectory() throws IOException;

    public File getProjectQCDirectory() throws IOException;

    public File getProjectFileDirectory() throws IOException;

    public File getProjectReferencesDirectory() throws IOException;

    public File getProjectJobDirectory() throws IOException;

    public String getDatabaseHost();

    public String getDatabaseName();

    @Override
    public void close();
}
