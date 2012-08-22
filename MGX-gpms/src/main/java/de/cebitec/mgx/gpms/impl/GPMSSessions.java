package de.cebitec.mgx.gpms.impl;

import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.mgx.gpms.util.DataSourceFactory;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.sql.DataSource;

/**
 *
 * @author sjaenick
 */
@Singleton
@Startup
public class GPMSSessions {

    private final static Logger logger = Logger.getLogger(GPMSSessions.class.getPackage().getName());
    private Map<DBMembershipI, GPMSMaster> sessions = null;

    @PostConstruct
    public void startup() {
        sessions = Collections.synchronizedMap(new HashMap<DBMembershipI, GPMSMaster>());
    }

    public GPMSMaster getMaster(DBMembershipI m) {

        GPMSMaster master;

        if (!sessions.containsKey(m)) {
            DataSource ds = DataSourceFactory.createDataSource(m);
            master = new GPMSMaster(m, ds);
            sessions.put(m, master);
        } else {
            System.err.println("using cached GPMSMaster, project "+ m.getProject().getName() + ", DS is "+ sessions.get(m).getDataSource().toString());
        }

        master = sessions.get(m);
        master.lastObtained(System.currentTimeMillis());
        return master;
    }

    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void timeout(Timer timer) {
        Set<DBMembershipI> remove = new HashSet<>();

        for (DBMembershipI m : sessions.keySet()) {
            GPMSMaster master = sessions.get(m);

            long IdleTime = (System.currentTimeMillis() - master.lastObtained()) / 1000;
            if (IdleTime > 60) {
                log("Removing session for " + master.getProject().getName() + " due to timeout (" + IdleTime + ")");
                remove.add(m);
            }
        }

        for (DBMembershipI m : remove) {
            GPMSMaster master = sessions.get(m);
            sessions.remove(m);
            master.close();
        }
    }

    private void log(String msg) {
        logger.log(Level.INFO, msg);
    }
}
