package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.api.MGXConfigurationI;
import de.cebitec.mgx.core.TaskI;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "TaskHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TaskHolder {

    @EJB
    MGXConfigurationI mgxconfig;
    @EJB
    Executor executor;
    private final int timeout = 60 * 60 * 24; // a day;
    private final ConcurrentMap<UUID, TaskI> tasks = new ConcurrentHashMap<>(10);

    public synchronized UUID addTask(final TaskI task) {
        final UUID uuid = UUID.randomUUID();
        tasks.put(uuid, task);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                task.setMainTask();
                task.run();
                task.close();
            }
        };
        executor.execute(r);
        return uuid;
    }

    public synchronized void removeTask(UUID uuid) {
        tasks.remove(uuid);
    }

    public TaskI getTask(UUID uuid) {
        return tasks.get(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "30", persistent = false)
    public void timeout() {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID uuid : tasks.keySet()) {
            TaskI s = tasks.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > timeout) {
                Logger.getLogger(TaskHolder.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), aborting task for {1}", new Object[]{timeout, s.getProjectName()});
                toRemove.add(uuid);
                s.cancel();
            }
        }

        for (UUID uuid : toRemove) {
            tasks.remove(uuid);
        }
    }
}
