package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.data.RoleI;

/**
 *
 * @author sjaenick
 */
public class Role implements RoleI {

    private String rolename;
    private String dbuser;
    private String dbpass;
    private GPMS gpms;

    public Role(GPMS gpms, String name, String user, String pass) {
        this.gpms = gpms;
        this.rolename = name;
        this.dbuser = user;
        this.dbpass = pass;
    }

    @Override
    public String getRolename() {
        return rolename;
    }

    @Override
    public String getDBPassword() {
        return dbpass;
    }

    @Override
    public String getDBUser() {
        return dbuser;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Role other = (Role) obj;
        if ((this.rolename == null) ? (other.rolename != null) : !this.rolename.equals(other.rolename)) {
            return false;
        }
        if (this.gpms != other.gpms && (this.gpms == null || !this.gpms.equals(other.gpms))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.rolename != null ? this.rolename.hashCode() : 0);
        hash = 83 * hash + (this.dbuser != null ? this.dbuser.hashCode() : 0);
        hash = 83 * hash + (this.dbpass != null ? this.dbpass.hashCode() : 0);
        hash = 83 * hash + (this.gpms != null ? this.gpms.hashCode() : 0);
        return hash;
    }
}
