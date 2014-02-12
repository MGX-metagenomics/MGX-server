package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import java.util.*;
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

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    private Map<UUID, UploadReceiverI> sessions = null;
    private int uploadTimeout;

    @PostConstruct
    public void start() {
        sessions = new HashMap<>();
        uploadTimeout = mgxconfig.getTransferTimeout();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            sessions.get(uuid).cancel();
            UploadReceiverI ur = sessions.remove(uuid);
            ur.cancel();
        }
    }

    public UUID registerUploadSession(UploadReceiverI recv) {
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, recv);
        return uuid;
    }

    public UploadReceiverI getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    //@Asynchronous -- not here
    public void closeSession(UUID uuid) throws MGXException {
        UploadReceiverI recv = sessions.remove(uuid);
        if (recv == null) {
            throw new MGXException("No active session for "+ uuid);
        }
        recv.close();
    }

    @Asynchronous
    public void cancelSession(UUID uuid) throws MGXException {
        sessions.get(uuid).cancel();
        sessions.remove(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void timeout(Timer timer) {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : sessions.keySet()) {
            UploadReceiverI s = sessions.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > uploadTimeout) {
                Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting upload session for {1}", new Object[]{uploadTimeout, s.getProjectName()});
                toRemove.add(uuid);
                s.cancel();
                
            }
        }
        
        for (UUID uuid : toRemove) {
            sessions.remove(uuid);
        }
    }
}
