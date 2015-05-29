package de.cebitec.mgx.jobsubmitter;

import de.cebitec.mgx.controller.MGXException;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author sjaenick
 */
public class JobParameterHelper {

    private JobParameterHelper() {
    }
    
    public static AutoCloseableIterator<JobParameter> getParameters(String toolData, File pluginDump) throws MGXException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            ToolDocumentHandler toolHandler = new ToolDocumentHandler(new Store());
            try (Reader r = new StringReader(toolData)) {
                parser.parse(new InputSource(r), toolHandler);
            }

            PluginDocumentHandler pluginHandler = new PluginDocumentHandler(toolHandler.getFilledStore());
            parser.parse(pluginDump, pluginHandler);
            Store store = pluginHandler.getFilledStore();
            return store.extractJobParameters();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(JobParameterHelper.class.getName()).log(Level.SEVERE, null, ex);
            throw new MGXException(ex);
        }
    }
}
