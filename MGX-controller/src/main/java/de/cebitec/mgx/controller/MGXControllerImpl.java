package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.JPAMasterI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.model.dao.*;
import de.cebitec.mgx.model.db.*;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
public class MGXControllerImpl implements MGXController {

    private final static Logger logger = Logger.getLogger(MGXController.class.getName());
    //
    private final String projectName;
    private final UserI user;
    //
    private final JPAMasterI gpmsmaster;
    private final EntityManager em;
    private File projectDir = null;
    private File projectQCDir = null;
    private File projectFileDir = null;
    private File projectJobDir = null;
    private File projectReferencesDir = null;
    //
    private final String persistentDir;
    //
    //
    private final static String DOUBLE_SEPARATOR = File.separator + File.separator;

    public MGXControllerImpl(JPAMasterI gpmsmaster, MGXConfigurationI cfg) {
        this.gpmsmaster = gpmsmaster;
        ProjectI gpmsProject = gpmsmaster.getProject();
        this.projectName = gpmsProject.getName();
        this.user = gpmsmaster.getUser();
        this.em = gpmsmaster.getEntityManagerFactory().createEntityManager();
        persistentDir = cfg.getPersistentDirectory();
    }

    @Override
    public final void log(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }
        logger.log(Level.INFO, "{0}/{1}: {2}", new Object[]{gpmsmaster.getProject().getName(), getCurrentUser(), msg});
    }

    @Override
    public final void log(String msg, Object... args) {
        log(String.format(msg, args));
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    @Override
    public File getProjectDirectory() throws IOException {
        if (projectDir != null) {
            return projectDir;
        }

        String ret = new StringBuilder(persistentDir).append(File.separator).append(getProjectName()).append(File.separator).toString();
        while (ret.contains(DOUBLE_SEPARATOR)) {
            ret = ret.replaceAll(DOUBLE_SEPARATOR, File.separator);
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
        projectDir = targetDir;
        return projectDir;
    }

    @Override
    public File getProjectQCDirectory() throws IOException {
        if (projectQCDir != null) {
            return projectQCDir;
        }

        File qcDir = new File(getProjectDirectory().getAbsolutePath() + File.separator + "QC");
        if (!qcDir.exists()) {
            UnixHelper.createDirectory(qcDir);
        }
        if (!UnixHelper.isGroupWritable(qcDir)) {
            UnixHelper.makeDirectoryGroupWritable(qcDir.getAbsolutePath());
        }
        projectQCDir = qcDir;
        return projectQCDir;
    }

    @Override
    public File getProjectReferencesDirectory() throws IOException {
        if (projectReferencesDir != null) {
            return projectReferencesDir;
        }

        File refDir = new File(getProjectDirectory().getAbsolutePath() + File.separator + "reference");
        if (!refDir.exists()) {
            UnixHelper.createDirectory(refDir);
        }
        if (!UnixHelper.isGroupWritable(refDir)) {
            UnixHelper.makeDirectoryGroupWritable(refDir.getAbsolutePath());
        }
        projectReferencesDir = refDir;
        return projectReferencesDir;
    }

    @Override
    public File getProjectFileDirectory() throws IOException {
        if (projectFileDir != null) {
            return projectFileDir;
        }

        File fileDir = new File(getProjectDirectory().getAbsolutePath() + File.separator + "files");
        if (!fileDir.exists()) {
            UnixHelper.createDirectory(fileDir);
        }
        if (!UnixHelper.isGroupWritable(fileDir)) {
            UnixHelper.makeDirectoryGroupWritable(fileDir.getAbsolutePath());
        }
        projectFileDir = fileDir;
        return projectFileDir;
    }

    @Override
    public File getProjectJobDirectory() throws IOException {
        if (projectJobDir != null) {
            return projectJobDir;
        }

        File jobDir = new File(getProjectDirectory().getAbsolutePath() + File.separator + "jobs");
        if (!jobDir.exists()) {
            UnixHelper.createDirectory(jobDir);
        }
        if (!UnixHelper.isGroupWritable(jobDir)) {
            UnixHelper.makeDirectoryGroupWritable(jobDir.getAbsolutePath());
        }
        projectJobDir = jobDir;
        return projectJobDir;
    }

    @Override
    public final DataSource getDataSource() {
        return gpmsmaster.getDataSource();
    }

    @Override
    public final Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    @Override
    public String getDatabaseHost() {
        return gpmsmaster.getGPMSDatasource().getHost().getHostName();
    }

    @Override
    public String getDatabaseName() {
        return gpmsmaster.getGPMSDatasource().getName();
    }
//
//    @Override
//    public String getJDBCUrl() {
//        return gpmsmaster.getProject().getDBConfig().getURI();
//    }

    @Override
    public void close() {
        if (em.isOpen()) {
            em.close();
        }
    }

    @Override
    public HabitatDAO<Habitat> getHabitatDAO() {
        return new HabitatDAO<>(this);
    }

    @Override
    public AttributeTypeDAO<AttributeType> getAttributeTypeDAO() {
        return new AttributeTypeDAO<>(this);
    }

    @Override
    public AttributeDAO<Attribute> getAttributeDAO() {
        return new AttributeDAO<>(this);
    }

    @Override
    public SampleDAO<Sample> getSampleDAO() {
        return new SampleDAO<>(this);
    }

    @Override
    public DNAExtractDAO<DNAExtract> getDNAExtractDAO() {
        return new DNAExtractDAO<>(this);
    }

    @Override
    public SeqRunDAO<SeqRun> getSeqRunDAO() {
        return new SeqRunDAO<>(this);
    }

    @Override
    public SequenceDAO<Sequence> getSequenceDAO() {
        return new SequenceDAO<>(this);
    }

    @Override
    public ToolDAO<Tool> getToolDAO() {
        return new ToolDAO<>(this);
    }

    @Override
    public JobDAO<Job> getJobDAO() {
        return new JobDAO<>(this);
    }

    @Override
    public JobParameterDAO<JobParameter> getJobParameterDAO() {
        return new JobParameterDAO<>(this);
    }

    @Override
    public ObservationDAO getObservationDAO() {
        return new ObservationDAO(this);
    }

    @Override
    public ReferenceDAO<Reference> getReferenceDAO() {
        return new ReferenceDAO<>(this);
    }

    @Override
    public MappingDAO<Mapping> getMappingDAO() {
        return new MappingDAO<>(this);
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getCurrentUser() {
        return user.getLogin();
    }

    @Override
    public RoleI getCurrentRole() {
        return gpmsmaster.getRole();
    }

//    private <T extends DAO> T getDAO(Class<T> clazz) {
//        if (!daos.containsKey(clazz)) {
//            daos.put(clazz, createDAO(clazz));
//        }
//        return (T) daos.get(clazz);
//    }
//
//    private <T extends DAO> T createDAO(Class<T> clazz) {
//        try {
//            Constructor<T> c = clazz.getConstructor();
//            T instance = c.newInstance(this);
//            //instance.setController(this);
//            return instance;
//        } catch (InstantiationException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        } catch (InvocationTargetException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        } catch (NoSuchMethodException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        } catch (SecurityException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        }
//        throw new UnsupportedOperationException("Could not create DAO " + clazz);
//    }
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.gpmsmaster);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MGXControllerImpl other = (MGXControllerImpl) obj;
        if (!Objects.equals(this.gpmsmaster, other.gpmsmaster)) {
            return false;
        }
        return true;
    }

}
