package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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