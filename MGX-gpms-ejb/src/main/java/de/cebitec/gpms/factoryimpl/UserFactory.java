
package de.cebitec.gpms.factoryimpl;

import de.cebitec.gpms.GPMS;
import de.cebitec.gpms.data.UserI;
import de.cebitec.mgx.gpms.impl.data.User;


/**
 *
 * @author sjaenick
 */
public class UserFactory {

    public static UserI getUser(GPMS gpms, String username) {
        return new User(gpms, username);
    }

}
