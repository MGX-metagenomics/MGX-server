package de.cebitec.mgx.controller;


import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.util.EMFNameResolver;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author sjaenick
 */
public class MGXPUResolver extends EMFNameResolver {

    @Override
    public boolean handles(MembershipI pm) {
        if (pm.getProject().getProjectClass().getName().equals("MGX")) {
            return true;
        }
        return false;
    }

    @Override
    public String resolve(MembershipI pm) {
        return "MGX";
    }

    @Override
    public EntityManagerFactory create(String pu, Properties config) {
        System.out.println("Creating EntityManagerFactory for "+ pu);
        return Persistence.createEntityManagerFactory(pu, config);
    }
}
