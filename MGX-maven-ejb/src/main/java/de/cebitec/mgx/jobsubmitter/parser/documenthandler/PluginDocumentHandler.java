package de.cebitec.mgx.jobsubmitter.parser.documenthandler;

import de.cebitec.mgx.jobsubmitter.data.impl.Node;
import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Für die in der Tool Datei gefundenen Knoten, findet diese Klasse die nötigen
 * Konfigurationen.
 *
 * @author belmann
 */
public class PluginDocumentHandler extends DefaultHandler {

    /**
     * Hier werden die Ids der Nodes aus dem Store abgespeichert, die den
     * gleichen ClassName haben wie der derzeitig geparste Node in der XML. Zu
     * den jeweiligen Nodes werden in dieser HashMap als value die ConfigItem
     * Namen abgespeichert.
     *
     */
    private Map<Long, String> idsAndConfigItems = new HashMap<>();
    /**
     * Sobald ein Node in der Plugin XML gefunden wurde, wird dieser Flag
     * gesetzt um dann nach den dazugehörigen configItems zu suchen.
     */
    private boolean searchItemConfig = false;
    /**
     * In dem Store werden die Nodes abgespeichert, die in dieser Klasse
     * bearbeitet werden.
     */
    private final Store store;

    /**
     *
     * Konstruktor mit dem Store der konfigurierbaren Knoten als Parameter.
     * searchItemConfig wird hier zunächst auf false gesetzt, solange kein
     * Knoten aus dem Store in der XML Datei gefunden wurde.
     *
     * @param lNodeTypes Store mit konfigurierbaren Knoten
     */
    public PluginDocumentHandler(Store lStore) {
        store = lStore;
    }

    /**
     * Wenn ein öffnender Tag geparst wird, wird auch diese Methode aufgerufen.
     *
     *
     * @param lUri Struktur der XML
     * @param lLocalName lokaler Name
     * @param lQName Name des Attributs
     * @param lAttributes Values der Attribute
     * @throws SAXException - Falls die xml nicht validiert werden kann.
     */
    @Override
    public final void startElement(String lUri, String lLocalName, String lQName,
            Attributes lAttributes)
            throws SAXException {
        super.startElement(lUri, lLocalName, lQName, lAttributes);

        if (!searchItemConfig) {
            if (TagsAndAttributes.nodetype.equals(lQName)) {
                idsAndConfigItems = new HashMap<>();
                searchItemConfig = searchNodes(lAttributes);
            }
        } else {
            if (TagsAndAttributes.config_item.equals(lQName) && containsConfigName(lAttributes.getValue(TagsAndAttributes.name))) {

                String configType = lAttributes.getValue(TagsAndAttributes.type);
                String optional = lAttributes.getValue(TagsAndAttributes.optional);
                
                setConfigAttributes(optional, configType);
            }

            if (TagsAndAttributes.choice.equals(lQName)) {

                String value = lAttributes.getValue(TagsAndAttributes.value);
                String valueDescription = lAttributes.getValue(TagsAndAttributes.description);

                for (Entry<Long, String> entry : idsAndConfigItems.entrySet()) {
                    Long id = entry.getKey();
                    String val = entry.getValue();
                    if (!val.isEmpty()) {
                        store.getNode(id).getConfigItem(val).getChoice().addItem(value, valueDescription);
                    }
                }
            }
        }
    }

    /**
     * Sucht im Store nach Nodes, die konfigurierbar sind und den gleichen Namen
     * haben, wie der derzeit geparste Node.
     *
     * @param lAttributes Attribute des XML Tag
     * @return Ob ein Node gefunden wurde oder nicht
     */
    private boolean searchNodes(Attributes lAttributes) {
        for (Entry<Long, Node> entry : store.entrySet()) {
            Node node = entry.getValue();
            if (lAttributes.getValue(TagsAndAttributes.classname).equals(node.getClassName())) {
                node.setDisplayName(lAttributes.getValue(TagsAndAttributes.displayname));
                idsAndConfigItems.put(entry.getKey(), "");
            }
        }

        return !idsAndConfigItems.isEmpty();
    }

    /**
     * Setzt die Attribute für ein ConfigItem eines konfigurierbaren Knoten
     *
     *
     * @param lOptional Ob der ConfigItem optional ist.
     * @param lConfigType Welchen Typ dieses ConfigItem hat.
     * besitzt.
     */
    private void setConfigAttributes(String lOptional, String lConfigType) {

        for (Entry<Long, String> entry : idsAndConfigItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Node node = store.getNode(entry.getKey());
                node.getConfigItem(entry.getValue())
                    .setConfigType(lConfigType)
                    .setOptional("1".equals(lOptional));
            }
        }
    }

    /**
     * Überprüft ob ein Node ein derzeit geparstes ConfigItem enthält.
     *
     * @param lConfigName Name des ConfigItems.
     * @return Ob ein ConfigItem gefunden wurde oder nicht.
     */
    private boolean containsConfigName(String lConfigName) {

        for (Long id : idsAndConfigItems.keySet()) {
            String configName = "";

            boolean containsConfigItem = store.getNode(id).containsConfigItem(lConfigName);

            if (containsConfigItem) {
                configName = store.getNode(id).getConfigItem(lConfigName).getConfigName();
            }
            idsAndConfigItems.put(id, configName);
        }

        for (String name : idsAndConfigItems.values()) {
            if (!name.isEmpty()) {
                return true;
            }
        }
        return false;
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

        if (searchItemConfig && TagsAndAttributes.configurations.equals(lQName)) {
            searchItemConfig = false;
        }
    }

    /**
     * Gibt den Store mit den konfigurierbaren Knoten wider.
     *
     * @return the nodesConfigurations
     */
    public Store getFilledStore() {
        return store;
    }
}
