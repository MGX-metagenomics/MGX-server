package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.model.dao.*;
import de.cebitec.mgx.model.db.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public interface MGXController extends AutoCloseable {

    public void log(String msg);

    public void log(String msg, Object... args);
    
    public MGXConfigurationI getConfiguration();

    public HabitatDAO<Habitat> getHabitatDAO();

    public AttributeTypeDAO<AttributeType> getAttributeTypeDAO();

    public AttributeDAO<Attribute> getAttributeDAO();

    public SampleDAO<Sample> getSampleDAO();

    public DNAExtractDAO<DNAExtract> getDNAExtractDAO();

    public SeqRunDAO<SeqRun> getSeqRunDAO();

    public SequenceDAO<Sequence> getSequenceDAO();

    public ToolDAO<Tool> getToolDAO();

    public JobDAO<Job> getJobDAO();

    public JobParameterDAO<JobParameter> getJobParameterDAO();

    public ObservationDAO getObservationDAO();

    public ReferenceDAO<Reference> getReferenceDAO();

    public MappingDAO<Mapping> getMappingDAO();

    public String getProjectName();

    public String getCurrentUser();

    public RoleI getCurrentRole();

    public EntityManager getEntityManager();

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
