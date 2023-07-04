package de.cebitec.mgx.sessions;

import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.util.AutoCloseableIterator;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashSet;
import java.util.Map;
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
@Singleton(mappedName = "IteratorHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class IteratorHolder {

    private final ConcurrentMap<UUID, TimeStamped<AutoCloseableIterator<?>>> content = new ConcurrentHashMap<>(10);

    private final int TIMEOUT_SECONDS = 2 * 60; // 2 minutes
    
    public synchronized <T> UUID add(final AutoCloseableIterator<T> iter) {
        final UUID uuid = UUID.randomUUID();
        content.put(uuid, new TimeStamped<>(iter));
        return uuid;
    }

    public synchronized void remove(UUID uuid) {
        content.remove(uuid);
    }

    @SuppressWarnings("unchecked")
    public <T> AutoCloseableIterator<T> get(UUID uuid) {
        if (!content.containsKey(uuid)) {
            return null;
        }
        return (AutoCloseableIterator<T>) content.get(uuid).getContent();
    }

    @Schedule(hour = "*", minute = "*", second = "5", persistent = false)
    public void timeout() {
        Set<UUID> toRemove = new HashSet<>();
        
        for (Map.Entry<UUID, TimeStamped<AutoCloseableIterator<?>>> me : content.entrySet()) {
            TimeStamped<AutoCloseableIterator<?>> ts = me.getValue();
                long sessionIdleTime = (System.currentTimeMillis() - ts.getLastAccessed()) / 1000;
                if (sessionIdleTime > TIMEOUT_SECONDS) {
                    Logger.getLogger(Sessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), closing iterator.", new Object[]{TIMEOUT_SECONDS});
                    toRemove.add(me.getKey());
                    ts.getContent().close(); // closes underlying db connection
                }
        }

        for (UUID uuid : toRemove) {
            content.remove(uuid);
        }
    }

    private static class TimeStamped<T> {

        private final T content;
        private long lastAccess;

        public TimeStamped(T content) {
            this.content = content;
            lastAccess = System.currentTimeMillis();
        }

        public T getContent() {
            lastAccess = System.currentTimeMillis();
            return content;
        }

        public long getLastAccessed() {
            return lastAccess;
        }

    }

}
