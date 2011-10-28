package de.cebitec.mgx.upload;

import de.cebitec.mgx.configuration.MGXConfiguration;
import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.SequenceDTOList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;

/**
 *
 * @author sjaenick
 */
@Singleton(mappedName = "UploadSessions")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class UploadSessions {

    @EJB(lookup = "java:global/MGX-maven-ear/MGX-maven-ejb/MGXConfiguration")
    MGXConfiguration mgxconfig;

    private Map<UUID, UploadReceiverI> sessions = null;
    private int uploadTimeout;

    @PostConstruct
    public void start() {
        sessions = new HashMap<UUID, UploadReceiverI>();
        uploadTimeout = mgxconfig.getUploadTimeout();
    }

    @PreDestroy
    public void stop() {
        for (UUID uuid : sessions.keySet()) {
            sessions.get(uuid).cancel();
            sessions.remove(uuid);
        }
    }

    public UUID registerUploadSession(UploadReceiverI recv) {
        UUID uuid = UUID.randomUUID();
        sessions.put(uuid, recv);
        return uuid;
    }

//    public UUID createSession(MGXController mgx, long seqrun_id) throws MGXException {
//        UUID uuid = UUID.randomUUID();
//        sessions.put(uuid, new SeqUploadReceiver<SequenceDTOList>(mgx.getJDBCUrl(), mgx.getProjectName(), seqrun_id));
//        return uuid;
//    }

    public void addData(UUID uuid, SequenceDTOList seqs) throws MGXException {
        sessions.get(uuid).add(seqs);
    }

    public void closeSession(UUID uuid) throws MGXException {
        sessions.get(uuid).close();
        sessions.remove(uuid);
    }

    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void timeout(Timer timer) {
        for (UUID uuid : sessions.keySet()) {
            UploadReceiverI s = sessions.get(uuid);
            long sessionIdleTime = (System.currentTimeMillis() - s.lastAccessed()) / 1000;
            if (sessionIdleTime > uploadTimeout) {
                Logger.getLogger(UploadSessions.class.getPackage().getName()).log(Level.INFO, "Timeout exceeded ({0} sec), killing upload session for {1}", new Object[]{uploadTimeout, s.getProjectName()});
                s.cancel();
                sessions.remove(uuid);
            }
        }
    }
}
