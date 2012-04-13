package de.cebitec.mgx.controller;


import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.util.EMFNameResolver;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author sjaenick
 */
public class MGXPUResolver extends EMFNameResolver {

    @Override
    public boolean handles(MembershipI pm) {
        return pm.getProject().getProjectClass().getName().equals("MGX");
    }

    @Override
    public String resolve(MembershipI pm) {
        return "MGX";
    }

    @Override
    public EntityManagerFactory create(String pu, Properties config) {
        Logger.getLogger(MGXPUResolver.class.getPackage().getName()).log(Level.INFO, "Creating EntityManagerFactory for {0}", pu);
        return Persistence.createEntityManagerFactory(pu, config);
    }
}
