package de.cebitec.mgx.controller;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author sjaenick
 */
@ApplicationScoped
public class ControllerFactory {

    @EJB
    DBGPMSI gpms;
    @EJB
    MGXConfigurationI mgxconfig;
    //
    //
    private final LoadingCache<JDBCMasterI, MGXControllerImpl> masterCache;
    private final Timer timer;

    public ControllerFactory() {

        masterCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<JDBCMasterI, MGXControllerImpl>() {
                    @Override
                    public void onRemoval(RemovalNotification<JDBCMasterI, MGXControllerImpl> notification) {
                        MGXController oldController = notification.getValue();
                        if (oldController != null) {
                            oldController.close();
                        }
                    }

                })
                .build(new CacheLoader<JDBCMasterI, MGXControllerImpl>() {
                    @Override
                    public MGXControllerImpl load(JDBCMasterI jdbcMaster) {
                            return new MGXControllerImpl(jdbcMaster, mgxconfig.getPluginDump(), mgxconfig.getPersistentDirectory());
                        }
                });

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                masterCache.cleanUp();
            }
        }, 10_000, 60_000 * 2);
    }

    @Produces
    @MGX
    @RequestScoped
    MGXController getController() {
        JDBCMasterI master = gpms.<JDBCMasterI>getCurrentMaster();
        if (master == null) {
            throw new IllegalArgumentException("GPMS did not provide a valid master!");
        }
        MGXControllerImpl ret = masterCache.getUnchecked(master);
        // update user info
        ret.setCurrentUser(master.getUser());
        return ret;
        //return new MGXControllerImpl(jpaMaster, mgxconfig);
    }

    @PreDestroy
    public void dispose() {
        timer.cancel();
        masterCache.invalidateAll();
    }

//    public void dispose(@Disposes MGXController c) {
//        try {
//            c.close();
//        } catch (Exception ex) {
//            Logger.getLogger(ControllerFactory.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
}
