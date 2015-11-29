package de.cebitec.mgx.gpms.impl.data;

import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.core.ProjectClassI;
import de.cebitec.gpms.core.UserI;
import de.cebitec.gpms.data.DBProjectI;
import de.cebitec.gpms.data.DBRoleI;
import de.cebitec.mgx.gpms.impl.GPMS;
import de.cebitec.mgx.gpms.impl.GPMSMaster;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class User implements UserI {

    private final String login;
    private final GPMS gpms;
    //
    private final static Map<UserI, List<MembershipI>> membership_cache = Collections.synchronizedMap(new HashMap<UserI, List<MembershipI>>());
    //
    private final static String sql = new StringBuilder("SELECT Project.name, Role.name from User ")
            .append("LEFT JOIN Member on (User._id = Member.user_id) ")
            .append("LEFT JOIN Project on (Member.project_id = Project._id) ")
            .append("LEFT JOIN Project_Class on (Project.project_class_id = Project_Class._id) ")
            .append("LEFT JOIN Role on (Member.role_id = Role._id) ")
            .append("WHERE User.login=? and Project_Class.name=?")
            .toString();

    public User(GPMS gpms, String login) {
        this.gpms = gpms;
        this.login = login;
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MembershipI> getMemberships(ProjectClassI projClass) {


        // cache lookup first
        //
        List<MembershipI> cachedMemberships = getCachedMemberships(this);
        if (cachedMemberships != null) {
            return cachedMemberships;
        }

        // no cache entry, have to do the lookup
        //
        List<MembershipI> ret = new ArrayList<>();
        List<DBRoleI> allroles = (List<DBRoleI>)projClass.getRoles();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = gpms.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, this.getLogin());
            stmt.setString(2, projClass.getName());
            rs = stmt.executeQuery();
            while (rs.next()) {

                DBProjectI project = new Project(gpms, rs.getString(1), projClass);

                String rolename = rs.getString(2);
                String dbuser = null;
                String dbpass = null;
                for (DBRoleI role : allroles) {
                    if (role.getName().equals(rolename)) {
                        dbuser = role.getDBUser();
                        dbpass = role.getDBPassword();
                    }
                }
                DBRoleI r = new Role(gpms, rolename, dbuser, dbpass);

                ret.add(new Membership(gpms, project, r));
            }
        } catch (SQLException ex) {
            Logger.getLogger(GPMSMaster.class.getPackage().getName()).log(Level.INFO, ex.getMessage());
            return ret; // empty list
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(GPMSMaster.class.getPackage().getName()).log(Level.INFO, ex.getMessage());
                return ret;
            }
        }

        // cache membership list
        setCachedMemberships(this, ret);

        return ret;
    }

    private static List<MembershipI> getCachedMemberships(UserI u) {
        if (membership_cache.containsKey(u)) {
            return membership_cache.get(u);
        }
        return null;
    }

    private static void setCachedMemberships(UserI u, List<MembershipI> m) {
        membership_cache.put(u, m);
    }

    public static void clearMembershipCache() {
        membership_cache.clear();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.login != null ? this.login.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if ((this.login == null) ? (other.login != null) : !this.login.equals(other.login)) {
            return false;
        }
        return true;
    }
}
