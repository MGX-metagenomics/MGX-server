package de.cebitec.mgx.sessions;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
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

    private ConcurrentMap<UUID, TaskI> tasks = new ConcurrentHashMap<>();
    
    public synchronized UUID addTask(final TaskI task) {
        final UUID uuid = UUID.randomUUID();
        tasks.put(uuid, task);
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
}
