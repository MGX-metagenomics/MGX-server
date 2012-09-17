package de.cebitec.mgx.jobsubmitter.data.impl;

import java.util.Comparator;

/**
 * Ein ConfigItem repräsentiert ein Feld, in dem ein Benutzer eine Auswahl
 * treffen kann.
 *
 *
 * @author belmann
 *
 */
public class ConfigItem implements Comparator<ConfigItem>, Comparable<ConfigItem> {

    /**
     * Setzt den String für die Antwort.
     *
     */
    private String answer;
    /**
     * Setzt die möglichen Auswahlwerte wenn eine Auswahl möglich ist.
     *
     */
    private Choices choice;
    /**
     * Speichert den Namen des ConfigItems.
     */
    private final String configName;
    /**
     * Speichert den Typ des ConfigItems. Der dem Namen in der plugin.xml
     * gleicht.
     *
     */
    private String configType;
    /**
     * Speichert den default Wert des ConfigItems.
     *
     */
    private String defaultValue;
    /**
     *
     * Speichert die Beschreibung des ConfigItems.
     *
     */
    private String userDescription;
    /**
     * Speichert, ob das ConfigItem optional ist oder nicht.
     */
    private boolean optional;
    /**
     * Speichert den vom User vergebenen Namen des ConfigItems.
     */
    private final String userName;

    /**
     * Der Konstruktor speichert den vom User vergebenen Namen, die Beschreibung
     * sowie den Namen des ConfigItems.
     *
     *
     * @param lUserName Der vom User vergebene Name.
     * @param lUserDescription Die vom User vergebene Beschreibung.
     * @param lConfigName Der Name des ConfigItems.
     */
    public ConfigItem(String lUserName, String lUserDescription, String lConfigName) {
        defaultValue = "";
        userName = lUserName;
        userDescription = lUserDescription;
        configName = lConfigName;
        optional = false;
        choice = new Choices();
        answer = "";
    }

    /**
     * Getter für den Namen des Configs. (Nicht der vom User vergebene Name.)
     *
     * @return Name des Configs.
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * Der Typ des ConfigItems.
     *
     * @return Der Typ des ConfigItems.
     */
    public String getConfigType() {
        return configType;
    }

    /**
     * Die vom User vergebene Beschreibung des ConfigItems.
     *
     * @return Die User Beschreibung des ConfigItems.
     */
    public String getUserDescription() {
        return userDescription;
    }

    /**
     * Abfrage, ob das ConfigItem optional ist oder nicht.
     *
     * @return Optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Gibt den vom User vergebenen Namen für das ConfigItem wieder.
     *
     * @return den vom User vergebenen Namen.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gibt bei Enumerations die Auflistung der einzelnen Objekte wieder, falls
     * diese für das ConfigItem überhaupt gesetzt sind.
     *
     * @return Auflistung von Objekten.
     */
    public Choices getChoice() {
        return choice;
    }

    /**
     * Überprüft ob die Antwort gesetzt ist oder nicht. Gibt auch false wieder,
     * wenn der String leer ist.
     *
     * @return Antwort gesetzt oder nicht.
     */
    public boolean isAnswerSet() {
        return answer != null && !answer.isEmpty();
    }

    /**
     * Setzt die Antwort.
     *
     *
     * @param lAnswer Die Antwort des Users.
     */
    public ConfigItem setAnswer(String lAnswer) {
        if (lAnswer.trim().isEmpty() || lAnswer == null) {
            answer = "";
        } else {
            this.answer = lAnswer;
        }
        return this;
    }

    /**
     * Gibt den Antwortstring wieder.
     *
     * @return Antwort.
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Setzt die mögliche Liste für eine Auswahl bei einer ComboBox.
     *
     *
     * @param choice Liste an Auswahlobjekten.
     */
    public ConfigItem setChoice(Choices lChoice) {
        choice = lChoice;
        return this;
    }

    /**
     * Gibt true wieder, falls eine Auswahl an Elementen für eine comboBox
     * vorhanden ist.
     *
     * @return
     */
    public boolean hasChoices() {
        return !choice.isEmpty();
    }

    /**
     * Setzt den Typen für ein ConfigItem.
     *
     * @param lConfigType Der Typ des ConfigItems.
     */
    public ConfigItem setConfigType(String lConfigType) {
        this.configType = lConfigType;
        return this;
    }

    /**
     * Setzt die Beschreibung des Benutzers für den ConfigItem.
     *
     * @param lUserDescription
     */
    public ConfigItem setUserDescription(String lUserDescription) {
        userDescription = lUserDescription;
        return this;
    }

    /**
     * Setzt den Wert, ob das ConfigItem optional ist oder nicht.
     *
     * @param lOptional optional
     */
    public ConfigItem setOptional(boolean lOptional) {
        this.optional = lOptional;
        return this;
    }

    /**
     * Getter für den default wert. Gibt leeren String wieder, wenn dieser nicht
     * gesetzt ist.
     *
     * @return defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gibt an, ob ein default Wert für dieses ConfigItem existiert oder nicht.
     *
     * @return Default Wert gesetzt oder nicht.
     */
    public boolean hasDefaultValue() {
        return !defaultValue.isEmpty();
    }

    /**
     * Setzt den default Wert für das ConfigItem.
     *
     * @param lDefaultValue the defaultValue to set
     */
    public ConfigItem setDefaultValue(String lDefaultValue) {
        this.defaultValue = lDefaultValue;
        return this;
    }

    /**
     * Vergleich von den optional Werten des ConfigItems. Dient zur Sortierung
     * der ConfigItems.
     *
     * @param o1 ConfigItem 1
     * @param o2 ConfigItem 2
     * @return ConfgItem 1 ist größer(1), kleiner(-1) oder gleich(0).
     */
    @Override
    public int compare(ConfigItem o1, ConfigItem o2) {
        return Boolean.compare(o1.optional, o2.optional);
    }

    /**
     * Vergleicht ein ConfigItem zu dem in dieser Klasse.
     *
     * @param o ConfigItem
     * @return ConfgItem 1 ist größer(1), kleiner(-1) oder gleich(0).
     */
    @Override
    public int compareTo(ConfigItem o) {
        return Boolean.compare(optional, o.optional);
    }
}
