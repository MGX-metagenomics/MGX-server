/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.model.db.JobParameter;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author sj
 */
public class ConveyorWorkflow extends DefaultHandler {

    private final String toolXML;
    private final PluginDump dump;
    private NodeType curToolNode;
    private final TLongObjectMap<NodeType> nodes = new TLongObjectHashMap<>();
    private final TLongSet endpoints = new TLongHashSet();

    public ConveyorWorkflow(String toolXML, PluginDump dump) {
        this.toolXML = toolXML;
        this.dump = dump;
    }

    public Collection<JobParameter> getParameters() throws WorkflowException {
        
        parse();
        
        final Collection<JobParameter> ret = new ArrayList<>();

        nodes.forEachEntry(new TLongObjectProcedure<NodeType>() {
            @Override
            public boolean execute(long nodeId, NodeType nodeType) {
                for (CfgItem item : nodeType.getConfigItems()) {
                    JobParameter jobParameter = new JobParameter()
                            .setParameterName(item.getName())
                            .setParameterValue(item.getValue())
                            .setClassName(nodeType.getClassName())
                            .setDisplayName(nodeType.getDisplayName())
                            .setDefaultValue(item.getDefaultValue())
                            .setNodeId(nodeId)
                            .setOptional(item.isOptional())
                            .setType(item.getType())
                            .setUserDescription(item.getUserDescription())
                            .setUserName(item.getUserName());

                    Map<String, String> choices = null;
                    for (Choice c : item.getChoices()) {
                        if (choices == null) {
                            choices = new HashMap<>();
                        }
                        choices.put(c.getValue(), c.getDescription());
                    }
                    jobParameter.setChoices(choices);

                    ret.add(jobParameter);
                }
                return true;
            }
        });

        return ret;
    }

    private void parse() throws WorkflowException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            try (Reader r = new StringReader(toolXML)) {
                parser.parse(new InputSource(r), this);
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new WorkflowException(ex);
        }

        //
        // check for a single node named "mgx"
        //
        int nodesNamedMGX = 0;
        for (NodeType nt : nodes.valueCollection()) {
            if ("mgx".equals(nt.getName()) && "Conveyor.MGX.GetMGXJob".equals(nt.getClassName())) {
                nodesNamedMGX++;
            }
        }
        if (nodesNamedMGX != 1) {
            throw new WorkflowException("Workflow has to contain one GetMGXJob node named \"mgx\"");
        }
        //
        // make sure all links point from/to valid nodes
        //
        boolean allOk = endpoints.forEach(new TLongProcedure() {
            @Override
            public boolean execute(long value) {
                return nodes.containsKey(value);
            }
        });
        if (!allOk) {
            throw new WorkflowException("Workflow contains connections from/to invalid nodes.");
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        super.startElement(uri, localName, qName, attributes);

        switch (qName) {
            case "node":
                final NodeType nodeTemplate = dump.getNodeTemplate(attributes.getValue("type"));
                if (nodeTemplate == null) {
                    throw new SAXException(attributes.getValue("type") + " is not a valid node type ");
                }
                curToolNode = new NodeType(nodeTemplate.getClassName());
                for (CfgItem item : nodeTemplate.getConfigItems()) {
                    try {
                        CfgItem clone = item.clone();
                        curToolNode.addItem(clone);
                    } catch (CloneNotSupportedException ex) {
                        curToolNode = null;
                    }
                }

                String nodeName = attributes.getValue("name");
                if (nodeName != null && !nodeName.isEmpty()) {
                    curToolNode.setName(nodeName);
                }

                nodes.put(Long.parseLong(attributes.getValue("id")), curToolNode);
                break;
            case "configuration_item":
                CfgItem item = null;
                for (CfgItem candidate : curToolNode.getConfigItems()) {
                    if (candidate.getName().equals(attributes.getValue("name"))) {
                        item = candidate;
                        break;
                    }
                }
                if (item == null) {
                    throw new SAXException(attributes.getValue("name") + " is not a valid configuration item for node type " + curToolNode.getClassName());
                }

                String itemValue = attributes.getValue("value");
                if (itemValue != null) {
                    Collection<Choice> choices = item.getChoices();
                    if (choices == null || choices.isEmpty()) {
                        item.setValue(itemValue);
                    } else {
                        // need to check supplied value against available choices
                        boolean choiceOk = false;
                        for (Choice c : choices) {
                            if (c.getValue().equals(itemValue)) {
                                choiceOk = true;
                                item.setValue(itemValue);
                                break;
                            }
                        }
                        if (!choiceOk) {
                            throw new SAXException(itemValue + "is not a valid choice for item " + attributes.getValue("name"));
                        }
                    }
                } else {
                    item.setValue(item.getDefaultValue());
                }
                item.setUserName(attributes.getValue("user_name"));
                item.setUserDescription(attributes.getValue("user_description"));
                break;
            case "link":
                String fromNode = attributes.getValue("from_node");
                String toNode = attributes.getValue("to_node");
                try {
                    long from = Long.parseLong(fromNode);
                    long to = Long.parseLong(toNode);
                    endpoints.add(from);
                    endpoints.add(to);
                } catch (NumberFormatException nfe) {
                    throw new SAXException("Invalid link endpoint ");
                }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        super.endElement(uri, localName, qName);

        switch (qName) {
            case "node":
                curToolNode = null;
                break;
        }
    }
}
