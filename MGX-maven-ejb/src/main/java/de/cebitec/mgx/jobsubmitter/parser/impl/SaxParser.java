
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.jobsubmitter.parser.impl;

//~--- non-JDK imports --------------------------------------------------------
import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.PluginDocumentHandler;
import de.cebitec.mgx.jobsubmitter.parser.documenthandler.ToolDocumentHandler;
import org.xml.sax.SAXException;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;

/**
 * Hier werden die Parser für die Tool XML Datei als auch die Plugin XMl datei
 * aufgerufen und verwaltet.
 *
 *
 * @author belmann
 */
public class SaxParser {

    private final static Logger LOGGER =
            Logger.getLogger(SaxParser.class.getName());
    /**
     * Der Parser für die jeweiligen Dateien.
     */
    private SAXParser parser;
    /**
     * Die Plugin Datei, wo alle möglichen Knoten aufgelistet sind
     */
    private File pluginFile;
    /**
     * EventHandler für die Plugin Datei.
     *
     */
    private PluginDocumentHandler pluginHandler;
    /**
     * Store für die einzelnen Knoten.
     */
    private Store store;
    /**
     * Tool Datei, die vom User erstellt wurde.
     */
    private File toolFile;
    /**
     * Handler, der die Events, die beim Parsen der Tool Datei entstehen
     * abfängt.
     */
    private ToolDocumentHandler toolHandler;

    /**
     * Konstruktor, der den Store und den Parser initialisiert.
     *
     *
     * @param toolXml Die vom User erstellte Tool Datei.
     * @param pluginsXml Die Plugins Datei mit allen möglichen Knoten.
     */
    public SaxParser() {
        store = new Store();

        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(Level.SEVERE, null,
                    ex);
        } catch (SAXException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }

    /**
     *
     * Gibt die konfigurierbaren Nodes mit ihren ConfigItems wieder.
     *
     * @param toolXml XML Datei mit den vom User zusammengestellten Tools.
     * @param pluginsXml Beinhaltet alle möglichen Nodes.
     * @return NodeStore mit allen konfigurierbaren Knoten.
     */
    public Store getNodesConfigurations(String toolXml,
            String pluginsXml) {

        store.removeAllNodes();
        toolFile = new File(toolXml);
        LOGGER.info("TOOL " + toolXml);
        loadData(toolXml);
        pluginFile = new File(pluginsXml);
        LOGGER.info("Plugin " + pluginsXml);
        parse();

        return store;
    }

    private static void loadData(String datName) {

        File file = new File(datName);

        if (!file.canRead() || !file.isFile()) {
        } else {

            FileReader fr = null;
            int c;
            StringBuffer buff = new StringBuffer();
            try {
                fr = new FileReader(file);
                while ((c = fr.read()) != -1) {
                    buff.append((char) c);
                }
                fr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * In der Methode wird die Tool-Datei als auch die Plugin-Datei geparst.
     *
     */
    private Store parse() {
        toolHandler = new ToolDocumentHandler(store);

        try {
            parser.parse(toolFile, toolHandler);
        } catch (SAXException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(
                    Level.SEVERE,
                    "Die XML-Datei des Tools konnte nicht geparst werden", ex);
        } catch (IOException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(Level.SEVERE, null,
                    ex);
        }

        pluginHandler =
                new PluginDocumentHandler(toolHandler.getFilledStore());

        try {
            parser.parse(pluginFile, pluginHandler);
        } catch (SAXException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(
                    Level.SEVERE,
                    "Die XML-Datei des Plugins konnte nicht geparst werden", ex);
        } catch (IOException ex) {
            Logger.getLogger(SAXParser.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
        return pluginHandler.getFilledStore();
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
