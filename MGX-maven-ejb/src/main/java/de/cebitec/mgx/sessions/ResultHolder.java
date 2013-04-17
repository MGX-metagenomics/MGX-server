package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.util.LimitingIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "ResultHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ResultHolder {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    private Map<UUID, LimitingIterator> sessions = null;
    private int timeout;

    @PostConstruct
    public void start() {
        sessions = new HashMap<>();
        timeout = mgxconfig.getTransferTimeout();
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

    public UUID add(UUID uuid, LimitingIterator session) {
        sessions.put(uuid, session);
        return uuid;
    }

    public LimitingIterator get(UUID uuid) {
        return sessions.get(uuid);
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
            LimitingIterator s = sessions.get(uuid);
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
