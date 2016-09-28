/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author sj
 */
public class PluginDump extends DefaultHandler {

    private final Collection<String> blackList;
    private final File pluginDump;
    private final Map<String, NodeType> nodes = new HashMap<>();
    private NodeType curNode = null;
    private CfgItem curCfgItem = null;
    private boolean parsed = false;

    public PluginDump(File pluginDump) {
        this(pluginDump, null);
    }

    public PluginDump(File pluginDump, Collection<String> blackList) {
        this.pluginDump = pluginDump;
        this.blackList = blackList;
    }
    
    public long lastModified() {
        return pluginDump.lastModified();
    }

    public void parse() throws SAXException, ParserConfigurationException, IOException {
        if (!parsed) {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(pluginDump, this);
            parsed = true;
        }
    }

    public NodeType getNodeTemplate(String className) {
        if (parsed && className != null && nodes.containsKey(className)) {
            return nodes.get(className);
        }
        return null;
    }
    
    public int size() {
        return nodes.size();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "nodetype":
                if (blackList == null || !blackList.contains(attributes.getValue("classname"))) {
                    curNode = new NodeType(attributes.getValue("classname"));
                    curNode.setDisplayName(attributes.getValue("displayname"));
                    curNode.setDescription(attributes.getValue("description"));
                    nodes.put(attributes.getValue("classname"), curNode);
                }
                break;
            case "config_item":
                curCfgItem = new CfgItem();
                curCfgItem.setName(attributes.getValue("name"));
                curCfgItem.setType(attributes.getValue("type"));
                curCfgItem.setDescription(attributes.getValue("description"));
                curCfgItem.setDefaultValue(attributes.getValue("default"));
                curCfgItem.setOptional("1".equals(attributes.getValue("optional")));
                curNode.addItem(curCfgItem);
                break;
            case "choice":
                Choice c = new Choice(attributes.getValue("value"), attributes.getValue("description"));
                curCfgItem.addChoice(c);
                break;

        }
        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);

        switch (qName) {
            case "nodetype":
                curNode = null;
                break;
            case "config_item":
                curCfgItem = null;
                break;
        }
    }
}
