package de.cebitec.mgx.global;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.model.dao.DAO;
import de.cebitec.mgx.model.dao.TermDAO;
import de.cebitec.mgx.model.dao.ToolDAO;
import de.cebitec.mgx.model.db.Term;
import de.cebitec.mgx.model.db.Tool;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "MGXGlobal")
@Startup
public class MGXGlobal {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration cfg;
    //
    private DataSource ds = null;
    private EntityManagerFactory emf;
    private EntityManager em;
    private Map<Class, DAO> daos;
    private final static Logger logger = Logger.getLogger(MGXGlobal.class.getPackage().getName());
    //
    private final static String DS_JNDI_NAME = "jdbc/MGXGlobal";

    public ToolDAO<Tool> getToolDAO() {
        return getDAO(ToolDAO.class);
    }

    public TermDAO<Term> getTermDAO() {
        if (!daos.containsKey(TermDAO.class)) {
            TermDAO dao = new TermDAO(ds);
            dao.setEntityManager(em);
            daos.put(TermDAO.class, dao);
        }
        return (TermDAO) daos.get(TermDAO.class);
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
            instance.setEntityManager(em);
            return instance;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        throw new UnsupportedOperationException("Could not create DAO " + clazz);
    }

    @PostConstruct
    public void start() {

        try {
            ds = InitialContext.<DataSource>doLookup(DS_JNDI_NAME);
        } catch (NamingException ex) {
        }

        if (ds == null) {
            BoneCPConfig bcfg = new BoneCPConfig();
            bcfg.setLazyInit(true);
            bcfg.setMaxConnectionsPerPartition(5);
            bcfg.setMinConnectionsPerPartition(1);
            bcfg.setPartitionCount(1);
            bcfg.setJdbcUrl(cfg.getMGXGlobalJDBCURL());
            bcfg.setUsername(cfg.getMGXGlobalUser());
            bcfg.setPassword(cfg.getMGXGlobalPassword());
            bcfg.setPoolName("MGX-Global");
            bcfg.setCloseConnectionWatch(false);
            bcfg.setMaxConnectionAgeInSeconds(600);

            ds = new BoneCPDataSource(bcfg);

            try {
                Context ctx = new InitialContext();
                ctx.bind(DS_JNDI_NAME, ds);
            } catch (NamingException ex) {
                log(ex.getMessage());
            }
        } else {
            log("Re-using old datasource for global zone.");
        }

        Map<String, String> globalCfg = new HashMap<>();
        globalCfg.put("javax.persistence.jtaDataSource", DS_JNDI_NAME);

        emf = Persistence.createEntityManagerFactory("MGX-global", globalCfg);
        em = emf.createEntityManager();

        daos = new HashMap<>();

        log("MGX global zone ready.");
    }

    @PreDestroy
    public void stop() {
        if (em != null && em.isOpen()) {
            em.close();
            em = null;
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }

        log("MGX global zone exiting..");
    }

    public void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
