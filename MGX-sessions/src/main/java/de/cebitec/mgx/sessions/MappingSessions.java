package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.MGXException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "MappingSessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class MappingSessions {

    @EJB
    MGXConfigurationI mgxconfig;
    private int timeout;
    private final ConcurrentMap<UUID, MappingDataSession> tasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        timeout = 60 * 60 * 24; // a day
    }

    @PreDestroy
    public synchronized void stop() {
        for (MappingDataSession mds : tasks.values()) {
            mds.close();
        }
        tasks.clear();
    }

    public synchronized UUID addSession(final MappingDataSession task) {
        final UUID uuid = UUID.randomUUID();
        tasks.put(uuid, task);
        return uuid;
    }

    public synchronized void removeSession(UUID uuid) throws MGXException {
        if (!tasks.containsKey(uuid)) {
            throw new MGXException("No mapping session for " + uuid);
        }
        MappingDataSession old = tasks.remove(uuid);
        old.close();
    }

    public synchronized void abort(long mappingId) {
        // called to abort an open session when a mapping object is deleted
        Set<UUID> toRemove = new HashSet<>();
        for (Entry<UUID, MappingDataSession> e : tasks.entrySet()) {
            if (e.getValue().getMappingId() == mappingId) {
                toRemove.add(e.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            MappingDataSession old = tasks.remove(uuid);
            old.close();
        }
    }

    public MappingDataSession getSession(UUID uuid) throws MGXException {
        if (!tasks.containsKey(uuid)) {
            throw new MGXException("No mapping session for " + uuid);
        }
        return tasks.get(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "30", persistent = false)
    public void timeout() {
        if (!tasks.isEmpty()) {
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : tasks.keySet()) {
                MappingDataSession s = tasks.get(uuid);
                long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
                if (sessionIdleTime > timeout) {
                    Logger.getLogger(MappingSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting mapping data session for {1}", new Object[]{timeout, s.getProjectName()});
                    toRemove.add(uuid);
                    s.close();
                }
            }

            for (UUID uuid : toRemove) {
                tasks.remove(uuid);
            }
        }
    }
}
