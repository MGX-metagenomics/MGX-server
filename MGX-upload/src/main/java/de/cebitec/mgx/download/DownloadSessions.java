package de.cebitec.mgx.download;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.upload.*;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "DownloadSessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DownloadSessions {

    @EJB
    MGXConfigurationI mgxconfig;
    private Map<UUID, DownloadProviderI<?>> sessions = null;
    private final int timeout = 60; //seconds

    @PostConstruct
    public void start() {
        sessions = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            sessions.remove(uuid).cancel();
        }
    }

    public UUID registerDownloadSession(DownloadProviderI<?> provider) {
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, provider);
        return uuid;
    }

    @SuppressWarnings("unchecked")
    public <T> DownloadProviderI<T> getSession(UUID uuid) throws MGXException {
        if (!sessions.containsKey(uuid)) {
            throw new MGXException("No such session: " + uuid.toString());
        }
        return (DownloadProviderI<T>) sessions.get(uuid);
    }

    @Asynchronous
    public void closeSession(UUID uuid) throws MGXException {
        if (!sessions.containsKey(uuid)) {
            throw new MGXException("No such session: " + uuid.toString());
        }
        sessions.remove(uuid).close();
    }

    @Asynchronous
    public void cancelSession(UUID uuid) throws MGXException {
        if (!sessions.containsKey(uuid)) {
            throw new MGXException("No such session: " + uuid.toString());
        }
        sessions.remove(uuid).cancel();
    }

    @Schedule(hour = "*", minute = "*", second = "10", persistent = false)
    public void timeout(Timer timer) {
        Collection<UUID> toRemove = null;
        for (Map.Entry<UUID, DownloadProviderI<?>> me : sessions.entrySet()) {
            DownloadProviderI<?> prov = me.getValue();
            long sessionIdleTime = (System.currentTimeMillis() - prov.lastAccessed()) / 1000;
            if (sessionIdleTime > timeout) {
                Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting download session for {1}", new Object[]{timeout, prov.getProjectName()});
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(me.getKey());
                prov.cancel();

            }
        }

        if (toRemove != null) {
            for (UUID uuid : toRemove) {
                sessions.remove(uuid);
            }
        }
    }
}
