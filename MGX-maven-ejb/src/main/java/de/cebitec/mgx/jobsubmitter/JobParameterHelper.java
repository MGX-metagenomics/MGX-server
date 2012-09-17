package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.PluginDocumentHandler;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.ToolDocumentHandler;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author sjaenick
 */
@Stateless(mappedName = "JobParameterHelper")
public class JobParameterHelper {

    public AutoCloseableIterator<JobParameter> getParameters(String toolData, File plugins) {
        Store store = null;
        try {
            store = computeParameters(toolData, plugins);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(JobParameterHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return store.extractJobParameters();
    }
    
    /**
     *
     * Gibt die konfigurierbaren Nodes mit ihren ConfigItems wieder.
     *
     * @param toolXml XML Datei mit den vom User zusammengestellten Tools.
     * @param pluginsXml Beinhaltet alle möglichen Nodes.
     * @return NodeStore mit allen konfigurierbaren Knoten.
     */
    private Store computeParameters(String toolXMLData, File pluginXMLFile) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ToolDocumentHandler toolHandler = new ToolDocumentHandler(new Store());
        try (Reader r = new StringReader(toolXMLData)) {
            parser.parse(new InputSource(r), toolHandler);
        }
        
        PluginDocumentHandler pluginHandler = new PluginDocumentHandler(toolHandler.getFilledStore());
        parser.parse(pluginXMLFile, pluginHandler);
        return pluginHandler.getFilledStore();
    }
}
