package de.cebitec.mgx.controller.boot;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.util.EMFNameResolver;

/**
 *
 * @author sjaenick
 */
public class MGXPUResolver extends EMFNameResolver {

    //private static final Logger LOG = Logger.getLogger(MGXPUResolver.class.getName());

    @Override
    public final boolean handles(MembershipI pm) {
        return pm.getProject().getProjectClass().getName().equals("MGX");
    }

    @Override
    public final String getPUName() {
        return "MGX-PU";
    }

//    @Override
//    public final EntityManagerFactory create(Properties config) {
//        LOG.log(Level.INFO, "Creating EntityManagerFactory for {0}", getPUName());
//        return Persistence.createEntityManagerFactory(getPUName(), config);
//    }
}
