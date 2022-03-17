package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.MGXException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "UploadSessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class UploadSessions {

    @EJB
    MGXConfigurationI mgxconfig;
    private Map<UUID, UploadReceiverI> sessions = null;
    private int uploadTimeout = 60; // seconds

    @PostConstruct
    public void start() {
        sessions = new ConcurrentHashMap<>();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            UploadReceiverI ur = sessions.remove(uuid);
            ur.cancel();
        }
    }

    public UUID registerUploadSession(UploadReceiverI recv) throws MGXException {
        if (recv == null) {
            throw new MGXException("Cannot register null session.");
        }
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, recv);
        return uuid;
    }

    @SuppressWarnings("unchecked")
    public <T> UploadReceiverI<T> getSession(UUID uuid) throws MGXException {
        if (!sessions.containsKey(uuid)) {
            throw new MGXException("No session for " + uuid);
        }
        return (UploadReceiverI<T>) sessions.get(uuid);
    }

    //@Asynchronous -- not here
    public void closeSession(UUID uuid) throws MGXException {
        UploadReceiverI recv = sessions.remove(uuid);
        if (recv == null) {
            throw new MGXException("No active session for " + uuid);
        }
        recv.close();
    }

    @Asynchronous
    public void cancelSession(UUID uuid) throws MGXException {
        UploadReceiverI recv = sessions.remove(uuid);
        if (recv != null) {
            recv.cancel();
        }
    }

    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void timeout(Timer timer) {
        Collection<UUID> toRemove = null;
        for (Map.Entry<UUID, UploadReceiverI> me : sessions.entrySet()) {
            UploadReceiverI recv = me.getValue();
            long sessionIdleTime = (System.currentTimeMillis() - recv.lastAccessed()) / 1000;
            if (sessionIdleTime > uploadTimeout) {
                Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting upload session for {1}", new Object[]{uploadTimeout, recv.getProjectName()});
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(me.getKey());
                recv.cancel();
            }
        }
        
        if (toRemove != null) {
            for (UUID uuid : toRemove) {
                sessions.remove(uuid);
            }
        }
    }
}
