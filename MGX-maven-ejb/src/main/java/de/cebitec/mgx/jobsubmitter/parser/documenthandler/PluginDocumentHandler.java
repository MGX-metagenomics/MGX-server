package de.cebitec.mgx.jobsubmitter.parser.documenthandler;

//~--- non-JDK imports --------------------------------------------------------
import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.jobsubmitter.parser.utilities.TagsAndAttributes;

import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

/**
 * Für die in der Tool Datei gefundenen Knoten, findet diese Klasse die nötigen
 * Konfigurationen.
 *
 * @author belmann
 */
public class PluginDocumentHandler extends DefaultHandler {

   private final static Logger LOGGER =
	 Logger.getLogger(PluginDocumentHandler.class.getName());
   /**
    * Hier werden die Ids der Nodes aus dem Store abgespeichert, die den
    * gleichen ClassName haben wie der derzeitig geparste Node in der XML. Zu
    * den jeweiligen Nodes werden in dieser HashMap als value die ConfigItem
    * Namen abgespeichert.
    *
    */
   private HashMap<String, String> idsAndConfigItems;
   /**
    * Sobald ein Node in der Plugin XML gefunden wurde, wird dieser Flag gesetzt
    * um dann nach den dazugehörigen configItems zu suchen.
    */
   private boolean searchItemConfig;
   /**
    * In dem Store werden die Nodes abgespeichert, die in dieser Klasse
    * bearbeitet werden.
    */
   private Store store;

   /**
    *
    * Konstruktor mit dem Store der konfigurierbaren Knoten als Parameter.
    * searchItemConfig wird hier zunächst auf false gesetzt, solange kein Knoten
    * aus dem Store in der XML Datei gefunden wurde.
    *
    * @param lNodeTypes Store mit konfigurierbaren Knoten
    */
   public PluginDocumentHandler(Store lStore) {
	searchItemConfig = false;
	store = lStore;
	idsAndConfigItems = new HashMap<String, String>();
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
	   if (lQName.equals(TagsAndAttributes.nodetype)) {
		idsAndConfigItems = new HashMap<String, String>();
		searchItemConfig = searchNodes(lAttributes);
	   }
	} else {
	   if (lQName.equals(TagsAndAttributes.config_item)
		 && containsConfigName(
		 lAttributes.getValue(TagsAndAttributes.name))) {

		String configType =
		    lAttributes.getValue(TagsAndAttributes.type);
		String optional =
		    lAttributes.getValue(TagsAndAttributes.optional);
		String defaultValue = lAttributes.getValue(TagsAndAttributes.def);

		setConfigAttributes(optional, configType,
		    defaultValue);
	   }

	   if (lQName.equals(TagsAndAttributes.choice)) {
		String value =
		    lAttributes.getValue(TagsAndAttributes.value);
		String valueDescription =
		    lAttributes.getValue(
		    TagsAndAttributes.description);

		for (String id : idsAndConfigItems.keySet()) {
		   if (!idsAndConfigItems.get(id).isEmpty()) {

			store.getNode(id).getConfigItem(idsAndConfigItems.get(id)).getChoice().addItem(value, valueDescription);

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
	Iterator iterator = store.getIterator();

	while (iterator.hasNext()) {

	   Map.Entry me = (Map.Entry) iterator.next();
	   String id = (String) me.getKey();

	   if (lAttributes.getValue(
		 TagsAndAttributes.classname).equals(
		 store.getNode(id).getClassName())) {

		store.getNode(id).setDisplayName(
		    lAttributes.getValue(
		    TagsAndAttributes.displayname));

		idsAndConfigItems.put(id, "");
	   }
	}

	if (idsAndConfigItems.isEmpty()) {
	   return false;
	} else {
	   return true;
	}

   }

   /**
    * Setzt die Attribute für ein ConfigItem eines konfigurierbaren Knoten
    *
    *
    * @param lOptional Ob der ConfigItem optional ist.
    * @param lConfigType Welchen Typ dieses ConfigItem hat.
    * @param lDefaultValue Ob und welchen default Wert dieses ConfigItem
    * besitzt.
    */
   private void setConfigAttributes(String lOptional, String lConfigType,
	 String lDefaultValue) {
	Boolean optionalBoolean;

	if (lOptional.equals("1")) {
	   optionalBoolean = true;
	} else {
	   optionalBoolean = false;
	}

	Set set = idsAndConfigItems.entrySet();
	Iterator iterator = set.iterator();
	Map.Entry me;
	String id;
	String configName;

	while (iterator.hasNext()) {

	   me = (Map.Entry) iterator.next();

	   configName = (String) me.getValue();

	   if (!(configName).isEmpty()) {
		id = (String) me.getKey();

		store.getNode(id).getConfigItem(configName).setConfigType(lConfigType);
		store.getNode(id).getConfigItem(configName).setOptional(optionalBoolean);
		store.getNode(id).getConfigItem(configName).setDefaultValue(lDefaultValue);
	   }
	}
   }

   /**
    * Überprüft ob ein Node ein derzeit geparstes ConfigItem enthält.
    *
    * @param lConfigName Name des ConfigItems.
    * @return Ob ein ConfigItem gefunden wurde oder nicht.
    */
   private Boolean containsConfigName(String lConfigName) {
	boolean containsConfigItem = false;
	String configName = "";

	for (String id : idsAndConfigItems.keySet()) {
	   configName = "";

	   containsConfigItem = store.getNode(id).containsConfigItem(lConfigName);


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
   public void endElement(String lUri, String lLocalName, String lQName)
	 throws SAXException {
	super.endElement(lUri, lLocalName, lQName);

	if (this.searchItemConfig == true) {
	   if (lQName.equals(TagsAndAttributes.configurations)) {
		searchItemConfig = false;
	   }
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


//~ Formatted by Jindent --- http://www.jindent.com
