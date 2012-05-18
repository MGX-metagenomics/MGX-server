
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.data.interf.NodeStore;
import de.cebitec.mgx.jobsubmitter.data.util.Transform;
import de.cebitec.mgx.jobsubmitter.parser.interf.Parser;
import de.cebitec.mgx.util.JobParameter;
import java.util.List;
import javax.ejb.Stateless;
import org.openide.util.Lookup;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    public List<JobParameter> getParameters(String tool, String plugins) {
      
        
        Parser parser = Lookup.getDefault().lookup(Parser.class);
        NodeStore store =parser.getNodesConfigurations(tool, plugins);
        
        return Transform.getFromNodeStoreJobParameter(store);
    }
    
}
