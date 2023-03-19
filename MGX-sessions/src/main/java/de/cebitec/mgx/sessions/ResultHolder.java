package de.cebitec.mgx.sessions;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.util.LimitingIterator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "ResultHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ResultHolder {

    private Map<UUID, LimitingIterator<?>> sessions = null;
    private final int timeout = 60;

    @PostConstruct
    public void start() {
        sessions = new HashMap<>(10);
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            try {
                sessions.get(uuid).close();
            } catch (Exception ex) {
                Logger.getLogger(ResultHolder.class.getName()).log(Level.SEVERE, null, ex);
            }
            sessions.remove(uuid);
        }
    }

    public <T> UUID add(UUID uuid, LimitingIterator<T> session) {
        sessions.put(uuid, session);
        return uuid;
    }

    @SuppressWarnings("unchecked")
    public <T> LimitingIterator<T> get(UUID uuid) {
        return (LimitingIterator<T>) sessions.get(uuid);
    }

    @Asynchronous
    public void close(UUID uuid) throws MGXException {
        try {
            sessions.get(uuid).close();
        } catch (Exception ex) {
            Logger.getLogger(ResultHolder.class.getName()).log(Level.SEVERE, null, ex);
        }
        sessions.remove(uuid);
    }

    @Asynchronous
    public void cancel(UUID uuid) throws MGXException {
        try {
            sessions.get(uuid).close();
        } catch (Exception ex) {
            Logger.getLogger(ResultHolder.class.getName()).log(Level.SEVERE, null, ex);
        }
        sessions.remove(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "10", persistent = false)
    public void timeout() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : sessions.keySet()) {
            LimitingIterator<?> s = sessions.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > timeout) {
                toRemove.add(uuid);
                try {
                    s.close();
                } catch (Exception ex) {
                    Logger.getLogger(ResultHolder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        for (UUID uuid : toRemove) {
            sessions.remove(uuid);
        }
    }
}
