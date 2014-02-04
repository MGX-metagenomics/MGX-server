package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.data.DBMasterI;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.global.MGXGlobal;
import de.cebitec.mgx.model.dao.*;
import de.cebitec.mgx.model.db.*;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author sjaenick
 */
public class MGXControllerImpl implements MGXController {

    private final static Logger logger = Logger.getLogger(MGXControllerImpl.class.getPackage().getName());
    private final DBMasterI gpmsmaster;
    private final EntityManager em;
    private final Map<Class, DAO> daos = new HashMap<>();
    private String projectDir = null;
    //
    private final MGXConfiguration config;
    private final MGXGlobal global;

    public MGXControllerImpl(DBMasterI gpmsmaster, MGXGlobal global, MGXConfiguration cfg) {
        this.gpmsmaster = gpmsmaster;
        this.global = global;
        this.config = cfg;
        this.em = gpmsmaster.getEntityManagerFactory().createEntityManager();
    }

    @Override
    public final void log(String msg) {
        if (msg != null) {
            if ("".equals(msg)) {
                return;
            }
            logger.log(Level.INFO, "{0}/{1}: {2}", new Object[]{gpmsmaster.getProject().getName(), getCurrentUser(), msg});
        }
    }
    
    @Override
    public final void log(String msg, Object... args) { 
        log(String.format(msg, args));
    }

    @Override
    public EntityManagerFactory getEMF() {
        assert false;
        return null; //return this.emf;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    @Override
    public MGXConfiguration getConfiguration() {
        return config;
    }

    @Override
    public MGXGlobal getGlobal() {
        return global;
    }

    @Override
    public String getProjectDirectory() {
        if (projectDir != null) {
            return projectDir;
        }
        
        String ret = new StringBuilder(getConfiguration().getPersistentDirectory()).append(File.separator).append(getProjectName()).append(File.separator).toString();
        while (ret.contains(File.separator + File.separator)) {
            ret = ret.replaceAll(File.separator + File.separator, File.separator);
        }

        // 
        File targetDir = new File(ret);
        if (!targetDir.exists()) {
            // make group writable directory
            UnixHelper.createDirectory(targetDir);
        }
        
        if (!UnixHelper.isGroupWritable(targetDir)) {
            UnixHelper.makeDirectoryGroupWritable(targetDir.getAbsolutePath());
        }
        
        projectDir = ret;
  
        return ret;
    }

    @Override
    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = gpmsmaster.getDataSource().getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(MGXControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }

    @Override
    public String getDatabaseHost() {
        return gpmsmaster.getProject().getDBConfig().getDatabaseHost();
    }

    @Override
    public String getDatabaseName() {
        return gpmsmaster.getProject().getDBConfig().getDatabaseName();
    }

    @Override
    public String getJDBCUrl() {
        return gpmsmaster.getProject().getDBConfig().getURI();
    }

    @Override
    public void close() {
        if (em.isOpen()) {
            em.close();
        }
    }

    @Override
    public HabitatDAO<Habitat> getHabitatDAO() {
        return getDAO(HabitatDAO.class);
    }

    @Override
    public AttributeTypeDAO<AttributeType> getAttributeTypeDAO() {
        return getDAO(AttributeTypeDAO.class);
    }

    @Override
    public AttributeDAO<Attribute> getAttributeDAO() {
        return getDAO(AttributeDAO.class);
    }

    @Override
    public SampleDAO<Sample> getSampleDAO() {
        return getDAO(SampleDAO.class);
    }

    @Override
    public DNAExtractDAO<DNAExtract> getDNAExtractDAO() {
        return getDAO(DNAExtractDAO.class);
    }

    @Override
    public SeqRunDAO<SeqRun> getSeqRunDAO() {
        return getDAO(SeqRunDAO.class);
    }

    @Override
    public SequenceDAO<Sequence> getSequenceDAO() {
        return getDAO(SequenceDAO.class);
    }

    @Override
    public ToolDAO<Tool> getToolDAO() {
        return getDAO(ToolDAO.class);
    }

    @Override
    public JobDAO<Job> getJobDAO() {
        return getDAO(JobDAO.class);
    }

    @Override
    public JobParameterDAO<JobParameter> getJobParameterDAO() {
        return getDAO(JobParameterDAO.class);
    }

    @Override
    public ObservationDAO getObservationDAO() {
        ObservationDAO dao = new ObservationDAO();
        dao.setController(this);
        return dao;
    }

    @Override
    public ReferenceDAO<Reference> getReferenceDAO() {
        return getDAO(ReferenceDAO.class);
    }

    @Override
    public MappingDAO<Mapping> getMappingDAO() {
        return getDAO(MappingDAO.class);
    }

    @Override
    public String getProjectName() {
        return gpmsmaster.getProject().getName();
    }

    @Override
    public String getCurrentUser() {
        return gpmsmaster.getLogin();
    }

    @Override
    public RoleI getCurrentRole() {
        return gpmsmaster.getRole();
    }

    private <T extends DAO> T getDAO(Class<T> clazz) {
        if (!daos.containsKey(clazz)) {
            daos.put(clazz, createDAO(clazz));
        }
        return (T) daos.get(clazz);
    }

    private <T extends DAO> T createDAO(Class<T> clazz) {
        try {
            Constructor<T> c = clazz.getConstructor();
            T instance = c.newInstance();
            instance.setController(this);
            return instance;
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        throw new UnsupportedOperationException("Could not create DAO " + clazz);
    }
}
