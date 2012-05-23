
package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.data.util.Transform;
import de.cebitec.mgx.jobsubmitter.parser.impl.SaxParser;
import de.cebitec.mgx.util.JobParameter;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.xml.parsers.SAXParser;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    private final static Logger LOGGER =
	 Logger.getLogger(JobParameterHelper.class.getName());
    
    public List<JobParameter> getParameters(String tool, String plugins) {   
        SaxParser parser = new SaxParser();
        Store store = parser.getNodesConfigurations(tool, plugins);   
        
        LOGGER.info("StoreSize: "+store.storeSize()+"");   
        
        List<JobParameter> parameters = Transform.getFromNodeStoreJobParameter(store);
        
        LOGGER.info("ParameterSize: "+parameters.size()+"");
        
        return parameters;
    } 
}
