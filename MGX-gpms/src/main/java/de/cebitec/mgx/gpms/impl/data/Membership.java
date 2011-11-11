package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.data.DBGPMSI;
import de.cebitec.gpms.data.DBMembershipI;
import de.cebitec.gpms.data.DBProjectI;
import de.cebitec.gpms.data.DBRoleI;

/**
 *
 * @author sjaenick
 */
public class Membership implements DBMembershipI {

    private DBProjectI project;
    private DBRoleI role;
    private DBGPMSI gpms;

    public Membership(DBGPMSI gpms, DBProjectI project, DBRoleI role) {
        this.gpms = gpms;
        this.project = project;
        this.role = role;
    }

    @Override
    public DBProjectI getProject() {
        return project;
    }

    @Override
    public DBRoleI getRole() {
        return role;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Membership other = (Membership) obj;
        if (this.project.getName().equals(other.project.getName()) && this.role.getName().equals(other.role.getName())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.project != null ? this.project.getName().hashCode() : 0);
        hash = 29 * hash + (this.role != null ? this.role.getName().hashCode() : 0);
        hash = 29 * hash + (this.gpms != null ? this.gpms.hashCode() : 0);
        return hash;
    }
}
