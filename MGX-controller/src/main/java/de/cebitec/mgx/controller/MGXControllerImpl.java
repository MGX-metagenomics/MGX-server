package de.cebitec.mgx.controller;

import de.cebitec.gpms.core.ProjectI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.util.GPMSManagedConnectionI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.model.dao.*;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class MGXControllerImpl implements MGXController {

    private final static Logger logger = Logger.getLogger(MGXController.class.getName());
    //
    private final String projectName;
    private UserI user;
    //
    private final File pluginDump;
    private final JDBCMasterI gpmsmaster;
    private final GPMSManagedDataSourceI dataSource;
    private File projectDir = null;
    private File projectQCDir = null;
    private File projectFileDir = null;
    private File projectJobDir = null;
    private File projectReferencesDir = null;
    //
    private final File persistentDir;
    //
    private final static String DOUBLE_SEPARATOR = File.separator + File.separator;

    public MGXControllerImpl(JDBCMasterI gpmsmaster, File pluginDump, File persistentDir) {
        this.gpmsmaster = gpmsmaster;
        this.dataSource = gpmsmaster.getDataSource();
        this.dataSource.subscribe(this);
        //
        ProjectI gpmsProject = gpmsmaster.getProject();
        this.projectName = gpmsProject.getName();
        this.user = gpmsmaster.getUser();
        this.pluginDump = pluginDump;
        this.persistentDir = persistentDir;
    }

    @Override
    public File getPluginDump() {
        return pluginDump;
    }

    @Override
    public final void log(Exception ex) {
        logger.log(Level.INFO, "{0}/{1}: {2}", new Object[]{gpmsmaster.getProject().getName(), getCurrentUser(), ex});
        if (ex.getCause() != null && ex.getCause() instanceof Exception) {
            log((Exception) ex.getCause());
        }
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
    public File getProjectDirectory() throws IOException {
        if (projectDir != null) {
            return projectDir;
        }

        String ret = new StringBuilder(persistentDir.getAbsolutePath()).append(File.separator).append(getProjectName()).append(File.separator).toString();
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
    public final GPMSManagedDataSourceI getDataSource() {
        return dataSource;
    }

    @Override
    public final GPMSManagedConnectionI getConnection() throws SQLException {
        return dataSource.getConnection(this);
    }

    @Override
    public String getDatabaseHost() {
        return gpmsmaster.getGPMSDatasource().getHost().getHostName();
    }

    @Override
    public int getDatabasePort() {
        return gpmsmaster.getGPMSDatasource().getHost().getPort();
    }

    @Override
    public String getDatabaseName() {
        return gpmsmaster.getGPMSDatasource().getName();
    }

    @Override
    public void close() {
        // jobdao instances are handled differently because they 
        // cache job parameters internally
        if (jobDAO != null) {
            jobDAO.dispose();
            jobDAO = null;
        }
        dataSource.close(this); // unsubscribe
    }

    @Override
    public HabitatDAO getHabitatDAO() {
        return new HabitatDAO(this);
    }

    @Override
    public AttributeTypeDAO getAttributeTypeDAO() {
        return new AttributeTypeDAO(this);
    }

    @Override
    public AttributeDAO getAttributeDAO() {
        return new AttributeDAO(this);
    }

    @Override
    public SampleDAO getSampleDAO() {
        return new SampleDAO(this);
    }

    @Override
    public DNAExtractDAO getDNAExtractDAO() {
        return new DNAExtractDAO(this);
    }

    @Override
    public SeqRunDAO getSeqRunDAO() {
        return new SeqRunDAO(this);
    }

    @Override
    public SequenceDAO getSequenceDAO() {
        return new SequenceDAO(this);
    }

    @Override
    public ToolDAO getToolDAO() {
        return new ToolDAO(this);
    }

    private JobDAO jobDAO = null;

    @Override
    public JobDAO getJobDAO() {
        if (jobDAO == null) {
            jobDAO = new JobDAO(this);
        }
        return jobDAO;
    }

    @Override
    public JobParameterDAO getJobParameterDAO() {
        return new JobParameterDAO(this);
    }

    @Override
    public ObservationDAO getObservationDAO() {
        return new ObservationDAO(this);
    }

    @Override
    public ReferenceDAO getReferenceDAO() {
        return new ReferenceDAO(this);
    }

    @Override
    public MappingDAO getMappingDAO() {
        return new MappingDAO(this);
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getCurrentUser() {
        return user.getLogin();
    }
    
    void setCurrentUser(UserI user) {
        this.user = user;
    }

    @Override
    public RoleI getCurrentRole() {
        return gpmsmaster.getRole();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.gpmsmaster);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MGXControllerImpl other = (MGXControllerImpl) obj;
        return Objects.equals(this.gpmsmaster, other.gpmsmaster);
    }

}
