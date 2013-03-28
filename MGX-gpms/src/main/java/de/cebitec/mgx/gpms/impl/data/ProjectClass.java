package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.RoleI;
import de.cebitec.mgx.gpms.GPMSException;
import de.cebitec.mgx.gpms.impl.GPMS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class ProjectClass implements ProjectClassI {
    
    private final String pclassname;
    private final GPMS gpms;
    private List<RoleI> roleList;
    
    public ProjectClass(GPMS gpms, String pclassname) throws GPMSException {
        this.pclassname = pclassname;
        this.gpms = gpms;
        this.roleList = loadRoles();
    }
    
    @Override
    public final String getName() {
        return pclassname;
    }
    
    @Override
    public final List<RoleI> getRoles() {
        return roleList;
    }
    
    private List<RoleI> loadRoles() throws GPMSException {
        List<RoleI> roles = new ArrayList<>();
        
        String cfgFile = new StringBuilder(gpms.getGPMSConfigDirectory()).append(File.separator).append(pclassname.toLowerCase()).append(".conf").toString();
        if (!new File(cfgFile).exists()) {
            throw new GPMSException(cfgFile + " missing or unreadable.");
        }
        
        System.err.println("reading MGX role file " + cfgFile);
        
        try {
            String line = null;
            boolean in_section = false;
            try (FileReader fr = new FileReader(cfgFile); BufferedReader br = new BufferedReader(fr)) {
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("<Role_Accounts>") != -1) {
                        in_section = true;
                        continue;
                    } else if (line.indexOf("</Role_Accounts>") != -1) {
                        in_section = false;
                        continue;
                    }
                    
                    if (in_section) {
                        line = line.trim();
                        String[] strings = line.split(":");
                        if (strings.length != 3) {
                            gpms.log("Unparseable line in " + cfgFile + ": " + line);
                            throw new GPMSException("Invalid format for application configuration file.");
                        }
                        
                        Role r = new Role(gpms, strings[0], strings[1], strings[2]);
                        roles.add(r);
                    }
                }
            }
        } catch (IOException ex) {
            throw new GPMSException(ex);
        }
        
        roleList = roles;
        
        return roles;
    }
}
