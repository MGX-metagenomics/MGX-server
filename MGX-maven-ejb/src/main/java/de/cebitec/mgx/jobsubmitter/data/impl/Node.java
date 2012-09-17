package de.cebitec.mgx.jobsubmitter.data.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ein Node repräsentiert eine sachliche Einheit, die ein oder mehrere Felder
 * (ConfigItems) beinhaltet, in denen der User eine Auswahl bzw Antwort geben
 * kann.
 *
 *
 * @author belmann
 */
public class Node implements Iterable<Entry<String, ConfigItem>> {

    /**
     * Der Klassenname eines Knotens.
     *
     */
    private final String className;
    /**
     * Da die ConfigItems alle eindeutig sind für einen Node, werden diese in
     * einer Map abgespeichert. Der Schlüssel ist hierbei der ConfigName.
     *
     */
    private final Map<String, ConfigItem> configItems;
    /**
     * Speichert den Namen des Nodes, der Angezeigt werden soll.
     */
    private String displayName;
    /**
     * Speichert die Id des Knotens.
     *
     */
    private final long id;

    /**
     * Der Konstruktor teilt den Klassennamen als auch die Id des Knotens den
     * Klassenvariablen zu und initialisiert die Map mit den ConfigItems.
     *
     *
     * @param lClassName Der Klassenname des Nodes.
     * @param lId Die Id des Nodes.
     */
    public Node(String lClassName, long lId) {
        id = lId;
        className = lClassName;
        configItems = new TreeMap<>();
    }

    /**
     * Gibt die Anzahl der ConfigItems des Nodes an.
     *
     * @return Anzahl an ConfigItems.
     */
    public int getNumberOfConfigItems() {
        return configItems.size();
    }

    /**
     * Gibt den Klassen des Nodes wieder.
     *
     * @return Klassenname des Nodes.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Überprüft, ob ein ConfigItem enthalten ist in der Map.
     *
     * @param lConfigName
     * @return Enthalten oder nicht.
     */
    public boolean containsConfigItem(String lConfigName) {
        return configItems.containsKey(lConfigName);
    }

    /**
     * Gibt das ConfigItem wieder.
     *
     * @param lConfigName Der Name des ConfigItems.
     * @return ConfigItem.
     */
    public ConfigItem getConfigItem(String lConfigName) {
        return configItems.get(lConfigName);
    }

    /**
     * Gibt die Antworten in einer Map zurück. Der Schlüssel, ist dabei der
     * eindeutige Name des ConfigItems. ConfigItems, dessen Antwort leer ist,
     * oder nicht gesetzt wurde, werden gelöscht.
     *
     * @return Map<String,String> mit allen Antworten.
     *
     *
     */
    public Map<String, String> getAnswers() {

        Map<String, String> map = new HashMap<>();
        for (Entry<String, ConfigItem> entry : configItems.entrySet()) {
            if (entry.getValue().isAnswerSet()) {
                map.put(entry.getKey(), entry.getValue().getAnswer());
            }
        }

        return map;
    }

    /**
     * Gibt einen Iterator wieder, um über alle ConfigItems iterieren zu können.
     *
     * @return Iterator
     */
    @Override
    public Iterator<Entry<String, ConfigItem>> iterator() {
        return configItems.entrySet().iterator();
    }

    public Set<Entry<String, ConfigItem>> entrySet() {
        return configItems.entrySet();
    }
    

    /**
     * Fügt ein ConfigItem zu dem Node hinzu.
     *
     *
     * @param lConfigItem
     */
    public Node addConfigItem(ConfigItem lConfigItem) {
        configItems.put(lConfigItem.getConfigName(), lConfigItem);
        return this;
    }

    /**
     * Gibt den Namen des Nodes, der angezeigt werden soll wieder.
     *
     * @return displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Setzt den Namen des Nodes, der angezeigt werden soll.
     *
     * @param lDisplayName displayName
     */
    public Node setDisplayName(String lDisplayName) {
        this.displayName = lDisplayName;
        return this;
    }

    /**
     * Gibt die eindeutige ID des Nodes wieder.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }
}
