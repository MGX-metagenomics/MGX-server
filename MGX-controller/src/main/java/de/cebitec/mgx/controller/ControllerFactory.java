package de.cebitec.mgx.controller;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.JPAMasterI;
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
    private LoadingCache<JPAMasterI, MGXController> masterCache = null;

    public ControllerFactory() {

        masterCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<JPAMasterI, MGXController>() {
                    @Override
                    public void onRemoval(RemovalNotification<JPAMasterI, MGXController> notification) {
                        MGXController oldController = notification.getValue();
                        if (oldController != null) {
                            oldController.close();
                        }
                    }

                })
                .build(
                        new CacheLoader<JPAMasterI, MGXController>() {
                    @Override
                    public MGXController load(JPAMasterI jpaMaster) {
                        return new MGXControllerImpl(jpaMaster, mgxconfig);
                    }
                });
    }

    @Produces
    @MGX
    @RequestScoped
    MGXController getController() {
        JPAMasterI jpaMaster = gpms.<JPAMasterI>getCurrentMaster();
        if (jpaMaster == null) {
            throw new IllegalArgumentException("GPMS did not provide a valid master!");
        }
        return masterCache.getUnchecked(jpaMaster);
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
