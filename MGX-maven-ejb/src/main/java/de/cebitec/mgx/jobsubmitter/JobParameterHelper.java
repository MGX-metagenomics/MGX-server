package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.data.util.Transform;
import de.cebitec.mgx.jobsubmitter.parser.impl.SaxParser;
import de.cebitec.mgx.model.db.JobParameter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    private final static Logger LOGGER =
            Logger.getLogger(JobParameterHelper.class.getName());

    public List<JobParameter> getParameters(String toolData, File plugins) {
        Store store = new Store();
        try {
            store = SaxParser.getNodesConfigurations(toolData, plugins);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(JobParameterHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        LOGGER.info("StoreSize: " + store.storeSize() + "");

        List<JobParameter> parameters = Transform.getFromNodeStoreJobParameter(store);

        LOGGER.info("ParameterSize: " + parameters.size() + "");

        return parameters;
    }
}
