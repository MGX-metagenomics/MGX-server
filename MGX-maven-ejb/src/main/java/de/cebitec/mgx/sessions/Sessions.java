package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
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
@Singleton(mappedName = "Sessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Sessions {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;
    private Map<UUID, TaskI> sessions = null;
    private int timeout;

    @PostConstruct
    public void start() {
        sessions = new HashMap<>(10);
        timeout = mgxconfig.getTransferTimeout();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            sessions.get(uuid).cancel();
            sessions.remove(uuid);
        }
    }

    public UUID registerSession(TaskI session) {
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, session);
        return uuid;
    }

    public TaskI getSession(UUID uuid) {
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
    public void timeout() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : sessions.keySet()) {
            TaskI s = sessions.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > timeout) {
                Logger.getLogger(Sessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting download session for {1}", new Object[]{timeout, s.getProjectName()});
                toRemove.add(uuid);
                s.cancel();
            }
        }

        for (UUID uuid : toRemove) {
            sessions.remove(uuid);
        }
    }
}
