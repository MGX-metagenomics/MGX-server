package de.cebitec.mgx.sessions;

import de.cebitec.mgx.configuration.MGXConfiguration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
@Singleton(mappedName = "TaskHolder")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TaskHolder {

    @EJB
    MGXConfiguration mgxconfig;
    private final int timeout = 60 * 60 * 24; // a day;
    private final ConcurrentMap<UUID, TaskI> tasks = new ConcurrentHashMap<>(10);

    public synchronized UUID addTask(final TaskI task) {
        final UUID uuid = UUID.randomUUID();
        tasks.put(uuid, task);
        
        // FIXME - leaking this thread?
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                task.setMainTask();
                task.run();
                task.close();
            }
        });
        t.start();
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
