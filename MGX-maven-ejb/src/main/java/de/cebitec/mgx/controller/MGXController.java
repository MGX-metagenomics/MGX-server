package de.cebitec.mgx.controller;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.dao.AttributeDAO;
import de.cebitec.mgx.model.dao.DNAExtractDAO;
import de.cebitec.mgx.model.dao.HabitatDAO;
import de.cebitec.mgx.model.dao.JobDAO;
import de.cebitec.mgx.model.dao.SampleDAO;
import de.cebitec.mgx.model.dao.SeqRunDAO;
import de.cebitec.mgx.model.dao.SequenceDAO;
import de.cebitec.mgx.model.dao.ToolDAO;
import de.cebitec.mgx.model.db.Attribute;
import de.cebitec.mgx.model.db.DNAExtract;
import de.cebitec.mgx.model.db.Habitat;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.Sample;
import de.cebitec.mgx.model.db.SeqRun;
import de.cebitec.mgx.model.db.Sequence;
import de.cebitec.mgx.model.db.Tool;
import java.sql.Connection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author sjaenick
 */
public interface MGXController {

    public void log(String msg);

    public void log(String msg, Object... args);

    public HabitatDAO<Habitat> getHabitatDAO();

    public AttributeDAO<Attribute> getAttributeDAO();

    public SampleDAO<Sample> getSampleDAO();

    public DNAExtractDAO<DNAExtract> getDNAExtractDAO();

    public SeqRunDAO<SeqRun> getSeqRunDAO();

    public SequenceDAO<Sequence> getSequenceDAO();

    public ToolDAO<Tool> getToolDAO();

    public JobDAO<Job> getJobDAO();

    public String getProjectName();

    public String getCurrentUser();

    public MGXConfiguration getConfiguration();

    public MGXGlobal getGlobal();

    public EntityManager getEntityManager();

    public EntityManagerFactory getEMF();

    public Connection getConnection();

    public String getProjectDirectory();

    public String getDatabaseHost();

    public String getDatabaseName();

    public String getJDBCUrl();

    public void close();
}
