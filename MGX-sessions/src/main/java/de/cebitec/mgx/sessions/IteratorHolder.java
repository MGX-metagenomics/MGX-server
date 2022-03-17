package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.util.AutoCloseableIterator;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "IteratorHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class IteratorHolder {

    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    Executor executor;
    private final ConcurrentMap<UUID, AutoCloseableIterator> content = new ConcurrentHashMap<>(10);

    public synchronized <T> UUID add(final AutoCloseableIterator<T> iter) {
        final UUID uuid = UUID.randomUUID();
        content.put(uuid, iter);
        return uuid;
    }

    public synchronized void remove(UUID uuid) {
        content.remove(uuid);
    }

    @SuppressWarnings("unchecked")
    public <T> AutoCloseableIterator<T> get(UUID uuid) {
        return (AutoCloseableIterator<T>) content.get(uuid);
    }

}
