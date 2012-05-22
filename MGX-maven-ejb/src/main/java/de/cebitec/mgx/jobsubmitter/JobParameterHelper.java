
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.data.interf.NodeStore;
import de.cebitec.mgx.jobsubmitter.data.util.Transform;
import de.cebitec.mgx.jobsubmitter.parser.impl.SaxParser;
import de.cebitec.mgx.jobsubmitter.parser.interf.Parser;
import de.cebitec.mgx.util.JobParameter;
import java.util.List;
import javax.ejb.Stateless;
import javax.xml.parsers.SAXParser;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    public List<JobParameter> getParameters(String tool, String plugins) {   
        SaxParser parser = new SaxParser();
        NodeStore store =parser.getNodesConfigurations(tool, plugins);       
        return Transform.getFromNodeStoreJobParameter(store);
    } 
}
