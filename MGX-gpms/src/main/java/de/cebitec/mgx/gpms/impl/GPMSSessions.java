package de.cebitec.mgx.gpms.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class GPMSSessions {

    private final static Logger logger = Logger.getLogger(GPMSSessions.class.getPackage().getName());
    private final Cache<MembershipI, MasterI> sessionsCache;

    public GPMSSessions() {
        sessionsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<MembershipI, MasterI>() {

                    @Override
                    public void onRemoval(RemovalNotification<MembershipI, MasterI> notification) {
                        MasterI master = notification.getValue();
                        log("Removing GPMS " + master.getRole().getName() + " session for " + master.getProject().getName());
                        master.close();
                    }
                })
                .build();
    }

    @PreDestroy
    public void stop() {
        sessionsCache.invalidateAll();
    }

    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void cleanup() {
        //
        // If expireAfterWrite or expireAfterAccess is requested entries may be evicted 
        // on each cache modification, on occasional cache accesses, or on calls to 
        // Cache.cleanUp(). Expired entries may be counted in Cache.size(), but will never
        // be visible to read or write operations.
        //
        sessionsCache.cleanUp();
    }

    public MasterI getMaster(MembershipI m) {
        return sessionsCache.getIfPresent(m);
    }

    public void registerMaster(MembershipI m, MasterI master) {
        //
        // a cached MasterI might already be present if a change in the
        // master class is requested
        //
        // see GPMS#createMaster
        //
        if (sessionsCache.getIfPresent(m) != null) {
            sessionsCache.invalidate(m);
        }
        sessionsCache.put(m, master);
    }

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
