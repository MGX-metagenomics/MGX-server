package de.cebitec.mgx.download;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.upload.*;
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
@Singleton(mappedName = "DownloadSessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DownloadSessions {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    private Map<UUID, DownloadProviderI> sessions = null;
    private int timeout;

    @PostConstruct
    public void start() {
        sessions = new HashMap<>();
        timeout = mgxconfig.getTransferTimeout();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            sessions.get(uuid).cancel();
            sessions.remove(uuid);
        }
    }

    public UUID registerDownloadSession(DownloadProviderI provider) {
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, provider);
        return uuid;
    }

    public <T> DownloadProviderI<T> getSession(UUID uuid) throws MGXException {
        if (!sessions.containsKey(uuid)) {
            throw new MGXException("No such session: " + uuid.toString());
        }
        return sessions.get(uuid);
    }

    @Asynchronous
    public void closeSession(UUID uuid) throws MGXException {
        sessions.get(uuid).close();
        sessions.remove(uuid);
    }

    @Asynchronous
    public void cancelSession(UUID uuid) throws MGXException {
        sessions.get(uuid).cancel();
        sessions.remove(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "10", persistent = false)
    public void timeout(Timer timer) {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : sessions.keySet()) {
            DownloadProviderI s = sessions.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > timeout) {
                Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting download session for {1}", new Object[]{timeout, s.getProjectName()});
                toRemove.add(uuid);
                s.cancel();

            }
        }

        for (UUID uuid : toRemove) {
            sessions.remove(uuid);
        }
    }
}
