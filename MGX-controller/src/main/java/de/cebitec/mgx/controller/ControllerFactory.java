package de.cebitec.mgx.controller;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

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
    private LoadingCache<JDBCMasterI, MGXController> masterCache = null;

    public ControllerFactory() {

        masterCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<JDBCMasterI, MGXController>() {
                    @Override
                    public void onRemoval(RemovalNotification<JDBCMasterI, MGXController> notification) {
                        MGXController oldController = notification.getValue();
                        if (oldController != null) {
                            oldController.close();
                        }
                    }

                })
                .build(
                        new CacheLoader<JDBCMasterI, MGXController>() {
                    @Override
                    public MGXController load(JDBCMasterI jpaMaster) {
                        return new MGXControllerImpl(jpaMaster, mgxconfig);
                    }
                });
    }

    @Produces
    @MGX
    @RequestScoped
    MGXController getController() {
        JDBCMasterI master = gpms.<JDBCMasterI>getCurrentMaster();
        if (master == null) {
            throw new IllegalArgumentException("GPMS did not provide a valid master!");
        }
        return masterCache.getUnchecked(master);
        //return new MGXControllerImpl(jpaMaster, mgxconfig);
    }

    public void dispose(@Disposes MGXController c) {
//        try {
//            c.close();
//        } catch (Exception ex) {
//            Logger.getLogger(ControllerFactory.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
