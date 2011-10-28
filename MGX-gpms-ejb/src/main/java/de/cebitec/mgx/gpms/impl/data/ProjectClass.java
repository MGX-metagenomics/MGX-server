package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.GPMSException;
import de.cebitec.gpms.data.ProjectClassI;
import de.cebitec.gpms.data.RoleI;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

        String cfgFile = new StringBuilder(gpms.getGPMSConfigDirectory()).append(File.separator).append(pclassname.toLowerCase()).append(".conf").toString();

        File f = new File(cfgFile);
        if (!f.exists()) {
            throw new GPMSException(cfgFile + " is missing.");
        }

        List<RoleI> roles = new ArrayList<RoleI>();

        String line = null;
        boolean in_section = false;

        System.err.println("reading MGX role file");

        try {
            FileReader fr = new FileReader(cfgFile);
            BufferedReader br = new BufferedReader(fr);
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

                    Role r = new Role(gpms, strings[0], strings[1], strings[2]);
                    roles.add(r);
                }
            }

            br.close();
            fr.close();
        } catch (Exception ex) {
            throw new GPMSException(ex.getMessage());
        }

        roleList = roles;

        return roles;
    }
}
