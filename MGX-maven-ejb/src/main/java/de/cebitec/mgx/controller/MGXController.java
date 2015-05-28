package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.model.dao.*;
import de.cebitec.mgx.model.db.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import javax.persistence.EntityManager;

/**
 *
 * @author sjaenick
 */
public interface MGXController extends AutoCloseable {

    public void log(String msg);

    public void log(String msg, Object... args);

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

    //public MGXConfiguration getConfiguration();
    //public MGXGlobal getGlobal();
    public EntityManager getEntityManager();

    //public EntityManagerFactory getEMF();
    public Connection getConnection();

    public File getProjectDirectory() throws IOException;

    public File getProjectQCDirectory() throws IOException;

    public File getProjectFileDirectory() throws IOException;

    public File getProjectReferencesDirectory() throws IOException;

    public File getProjectJobDirectory() throws IOException;

    public String getDatabaseHost();

    public String getDatabaseName();

    public String getJDBCUrl();

    //public void close();
}
