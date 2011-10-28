package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.GPMSMasterI;
import de.cebitec.gpms.data.MembershipI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class GPMSSessions {

    private final static Logger logger = Logger.getLogger(GPMSSessions.class.getPackage().getName());
    private Map<MembershipI, GPMSMaster> sessions = null;

    @PostConstruct
    public void startup() {
        sessions = new HashMap<MembershipI, GPMSMaster>();
    }

    public GPMSMasterI getMaster(MembershipI m) {

        GPMSMaster master;

        if (!sessions.containsKey(m)) {
            // create new gpmsmaster
            master = new GPMSMaster(m, "MGX-PU");
            sessions.put(m, master);
        }

        master = sessions.get(m);
        master.lastObtained(System.currentTimeMillis());
        return master;
    }

    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void timeout(Timer timer) {
        for (MembershipI m : sessions.keySet()) {
            GPMSMaster master = sessions.get(m);

            long IdleTime = (System.currentTimeMillis() - master.lastObtained()) / 1000;
            if (IdleTime > 60) {
                log("Removing session for " + master.getProjectName() + " due to timeout ("+IdleTime+")");
                sessions.remove(m);
                master.close();
            }
        }
    }

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }

}
