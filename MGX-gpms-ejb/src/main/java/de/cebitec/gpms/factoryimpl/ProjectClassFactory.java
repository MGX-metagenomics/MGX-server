package de.cebitec.gpms.factoryimpl;

import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.GPMSException;
import de.cebitec.gpms.data.ProjectClassI;
import de.cebitec.gpms.common.ProjectClassFactoryI;
import de.cebitec.mgx.gpms.impl.data.ProjectClass;
import java.util.HashMap;

/**
 *
 * @author sjaenick
 */
public class ProjectClassFactory implements ProjectClassFactoryI {

    private static HashMap<String, ProjectClassI> cache = new HashMap<String, ProjectClassI>();

    @Override
    public ProjectClassI getProjectClass(GPMS gpms, String projClass) {
        try {
            if (cache.containsKey(projClass))
                return cache.get(projClass);

            return cache.put(projClass, new ProjectClass(gpms, projClass));
        } catch (GPMSException ex) {
            return null;
        }
    }
}
