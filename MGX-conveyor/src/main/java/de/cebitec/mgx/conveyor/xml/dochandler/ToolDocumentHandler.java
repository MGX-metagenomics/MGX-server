package de.cebitec.mgx.conveyor.xml.dochandler;

import de.cebitec.mgx.conveyor.ConfigItem;
import de.cebitec.mgx.conveyor.Node;
import de.cebitec.mgx.conveyor.Store;
import de.cebitec.mgx.conveyor.TagsAndAttributes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Diese Klasse filtert die konfigurierbaren Knoten aus der vom User erstellten,
 * XML Datei.
 *
 * @author belmann
 */
public class ToolDocumentHandler extends DefaultHandler {

    /**
     * Sobald eine neue Node gefunden wird, muss diese angelegt werden, was
     * durch diesen boolean markiert wird.
     */
    private boolean firstConfigFlag = false;
    /**
     * CurrentNode speichert den aktuell zu bearbeitenden Knoten.
     *
     */
    private Node currentNode;
    /**
     * CurrentNodeType speichert den Typ des aktuell zu bearbeitenden Knoten.
     *
     */
    private String currentNodeType;
    /**
     * CurrendNodeId speichert die Id des aktuell zu bearbeitenden Knoten.
     */
    private Long currentNodeId;
    /**
     * Sobald ein endElement String von configuration_item erreicht wird, wird
     * durch den Flag angegeben, dass der Node gespeichert werden soll.
     */
    private boolean save = false;
    /**
     * Der Store speichert die gefundenen Nodes und ConfigItems.
     */
    private final Store store;

    public ToolDocumentHandler(Store lStore) {
        store = lStore;
    }

    public Store getFilledStore() {
        return store;
    }

    /**
     *
     * Wird am Anfang eines XML Elements aufgerufen. Hier werden die Nodes
     * gefunden und die ConfigItems im zugehörigen Node abgespeichert.
     *
     * @param lUri Struktur der XML
     * @param lLocalName lokale Name
     * @param lQName Namen des Attributs
     * @param lAttributes Values der Attribute
     * @throws SAXException - Falls die xml nicht validiert werden kann.
     */
    @Override
    public void startElement(String lUri, String lLocalName, String lQName, Attributes lAttributes) throws SAXException {

        super.startElement(lUri, lLocalName, lQName, lAttributes);

        if (lQName.equals(TagsAndAttributes.node)) {
            currentNodeType = lAttributes.getValue(TagsAndAttributes.type);
            currentNodeId = Long.parseLong(lAttributes.getValue(TagsAndAttributes.id));
            firstConfigFlag = true;

        } else if (lAttributes.getValue(TagsAndAttributes.user_description) != null) {

            if (firstConfigFlag) {
                currentNode = new Node(currentNodeType, currentNodeId);
                firstConfigFlag = false;
            }

            String user_name = lAttributes.getValue(TagsAndAttributes.user_name);
            String user_description = lAttributes.getValue(TagsAndAttributes.user_description);
            String configName = lAttributes.getValue(TagsAndAttributes.name);
            String defaultValue = lAttributes.getValue(TagsAndAttributes.def);
            if (defaultValue == null) {
                defaultValue = "";

            }


            currentNode.addConfigItem(new ConfigItem(user_name, user_description, configName, defaultValue));
            save = true;
        }
    }

    /**
     * Sobald ein XML Tag Element endet, wird diese Methode aufgerufen.
     *
     * @param lUri Struktur der XML
     * @param lLocalName lokaler Name
     * @param lQName Name des Attributs
     * @throws SAXException - Falls die xml nicht validiert werden kann.
     */
    @Override
    public void endElement(String lUri, String lLocalName, String lQName) throws SAXException {
        super.endElement(lUri, lLocalName, lQName);

        if (save && lQName.equals(TagsAndAttributes.configuration_items)) {
            store.addNode(currentNode);
            save = false;
        }
    }
}
