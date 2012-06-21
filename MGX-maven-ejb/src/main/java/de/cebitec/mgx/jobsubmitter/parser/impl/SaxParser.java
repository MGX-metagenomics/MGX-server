package de.cebitec.mgx.jobsubmitter.parser.impl;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.PluginDocumentHandler;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.ToolDocumentHandler;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Hier werden die Parser für die Tool XML Datei als auch die Plugin XMl datei
 * aufgerufen und verwaltet.
 *
 *
 * @author belmann
 */
public final class SaxParser {

    private SaxParser() {
        // static class
    }

    /**
     *
     * Gibt die konfigurierbaren Nodes mit ihren ConfigItems wieder.
     *
     * @param toolXml XML Datei mit den vom User zusammengestellten Tools.
     * @param pluginsXml Beinhaltet alle möglichen Nodes.
     * @return NodeStore mit allen konfigurierbaren Knoten.
     */
    
    
    public static Store getNodesConfigurations(String toolXml, String pluginsXml) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        Store store = new Store();

        File toolFile = new File(toolXml);
        File pluginFile = new File(pluginsXml);

        ToolDocumentHandler toolHandler = new ToolDocumentHandler(store);

        parser.parse(toolFile, toolHandler);

        PluginDocumentHandler pluginHandler = new PluginDocumentHandler(toolHandler.getFilledStore());

        parser.parse(pluginFile, pluginHandler);
        return pluginHandler.getFilledStore();
    }
}
