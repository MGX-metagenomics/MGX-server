package de.cebitec.mgx.jobsubmitter.data.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Verwaltet die Nodes einer vom User möglichen Auswahl an Tools.
 *
 *
 * @author belmann
 */
public class Store {

    /**
     * Speichert die Nodes bzw. Tools in einer HashMap. Dabei stellen die id den
     * key dar.
     */
    private Map<String, Node> nodes = new TreeMap<>();

    /**
     * Gibt einen Iterator wieder, um über alle Nodes iterieren zu können.
     *
     * @return Iterator
     */
    public Iterator<Entry<String, Node>> getIterator() {
        return nodes.entrySet().iterator();
    }

    /**
     * Gibt ein Node wieder.
     *
     * @param lId Id des Nodes.
     * @return Node
     */
    public final Node getNode(String lId) {
        return nodes.get(lId);
    }

    /**
     * Fügt einen weiteren Knoten hinzu.
     *
     * @param node
     */
    public final void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    /**
     * Gibt die Anzahl der bisherigen Knoten im Store wieder.
     *
     * @return Anzahl Knoten.
     */
    public int storeSize() {
        return nodes.size();
    }

    /**
     * Entfernt alle Knoten, bei denen keine Antwort gesetzt wurde.
     */
    public void deleteEmptyNodes() {
        for (Entry<String, Node> e : nodes.entrySet()) {
            e.getValue().deleteEmptyConfigItems();
            if (e.getValue().getNumberOfConfigItems() == 0) {
                nodes.remove(e.getKey());
            }
        }
    }

    /**
     * Entfernt einen Knoten aus dem Store.
     *
     * @param id
     */
    public void removeNode(String id) {
        nodes.remove(id);
    }

    /**
     * Entfernt alle Nodes aus dem Store.
     */
    public void removeAllNodes() {
        nodes.clear();
    }

    /**
     * Gibt alle Antworten in einer HashMap wieder. Als Key, dient der Name des
     * ConfigItems.
     *
     * @return ConfigItem
     */
    public HashMap<String, HashMap<String, String>> getAllAnswers() {
        deleteEmptyNodes();

        HashMap<String, HashMap<String, String>> map = new HashMap<>();

        for (String key : nodes.keySet()) {
            map.put(key, nodes.get(key).getAnswers());
        }
        return map;
    }
}
