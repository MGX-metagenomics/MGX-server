package de.cebitec.mgx.jobsubmitter.parser.impl;

import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.PluginDocumentHandler;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.ToolDocumentHandler;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public static Store getNodesConfigurations(String toolXMLData, File pluginXMLFile) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ToolDocumentHandler toolHandler = new ToolDocumentHandler(new Store());

        parser.parse(toolXMLData, toolHandler);

        PluginDocumentHandler pluginHandler = new PluginDocumentHandler(toolHandler.getFilledStore());
        parser.parse(pluginXMLFile, pluginHandler);
        return pluginHandler.getFilledStore();
    }

//    public static Store getNodesConfigurations(File toolXMLFile, File pluginXMLFile) throws ParserConfigurationException, SAXException, IOException {
//        String XMLData = readFile(toolXMLFile);
//        return getNodesConfigurations(XMLData, pluginXMLFile);
//    }

}
