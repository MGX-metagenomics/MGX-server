
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.util.JobParameter;
import java.util.List;
import javax.ejb.Stateless;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    public List<JobParameter> getParameters(String tool, String plugins) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
