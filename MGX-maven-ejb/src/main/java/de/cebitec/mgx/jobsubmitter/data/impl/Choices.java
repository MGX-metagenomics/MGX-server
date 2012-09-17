
package de.cebitec.mgx.jobsubmitter.data.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bei Choices, kann der User die einzelnen Elemente über eine ComboBox
 * auswählen.
 *
 *
 * @author belmann
 */
public class Choices {

    /**
     * Hier sind die einzelnen Elemente gespeichert, dabei dient der choice Name
     * als key und die Beschreibung als value.
     */
    private Map<String, String> sample;

    public Choices() {
        sample = new LinkedHashMap<>();
    }

    public Choices(Map<String, String> choices) {
        sample = choices;
    }

    /**
     * Fügt ein Element hinzu.
     *
     * @param value Name
     * @param description Beschreibung
     */
    public void addItem(String value, String description) {
        sample.put(value, description);
    }

    /**
     * Überprüft, ob eine Auswahl vorhanden ist oder nicht.
     *
     * @return Auswahl vorhanden oder nicht
     */
    public boolean isEmpty() {
        return sample.isEmpty();
    }

    /**
     * Gibt die Auswahl für das ConfigItem wieder.
     *
     * @return LinkedHashMap, wobei der Schlüssel, das Value darstellt.
     */
    public Map<String, String> getChoices() {
        return sample;
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
