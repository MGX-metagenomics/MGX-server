package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.MGXException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;

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
    private int uploadTimeout;

    @PostConstruct
    public void start() {
        sessions = new ConcurrentHashMap<>();
        uploadTimeout = mgxconfig.getTransferTimeout();
    }

    @PreDestroy
    public void stop() {
        Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Undeploying MGX, aborting all pending upload sessions..");

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
