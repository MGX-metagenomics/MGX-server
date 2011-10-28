package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.data.MembershipI;
import de.cebitec.gpms.data.ProjectI;
import de.cebitec.gpms.data.RoleI;

/**
 *
 * @author sjaenick
 */
public class Membership implements MembershipI {

    private ProjectI project;
    private RoleI role;
    private GPMS gpms;

    public Membership(GPMS gpms, ProjectI project, RoleI role) {
        this.gpms = gpms;
        this.project = project;
        this.role = role;
    }

    @Override
    public ProjectI getProject() {
        return project;
    }

    @Override
    public RoleI getRole() {
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
        if (this.project.getName().equals(other.project.getName()) && this.role.getRolename().equals(other.role.getRolename())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.project != null ? this.project.getName().hashCode() : 0);
        hash = 29 * hash + (this.role != null ? this.role.getRolename().hashCode() : 0);
        hash = 29 * hash + (this.gpms != null ? this.gpms.hashCode() : 0);
        return hash;
    }
}
