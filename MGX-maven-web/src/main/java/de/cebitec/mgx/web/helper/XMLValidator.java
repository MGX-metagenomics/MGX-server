/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.web.helper;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validiert ob, die XML wohlgeformt ist und ob der erste Knoten den String
 * "Conveyor.MGX.GetMGXJob" bei dem Attribut type enthaelt.
 *
 *
 * @author pbelmann
 */
public class XMLValidator {

    /**
     * ToolDocumentHandler fuer das melden von einzelnen Bestandteilen.
     */
    private ToolDocumentHandler handler;
    
    /**
     * Sobald der "nodes" startet, wird dieses Flag auf true gesetzt.
     */
    private boolean nodesStart;
    
    /**
     * Falls die Bedingung eintritt, dass XML wohlgeformt ist und der erste
     * Knoten den String "Conveyor.MGX.GetMGXJob" bei dem Attribut type
     * enthaelt.
     *
     */
    private boolean valid;

    /**
     *
     * Konstruktor fuer das erzeugen von Objekten.
     *
     */
    public XMLValidator() {
        nodesStart = false;
        valid = false;
        handler = new ToolDocumentHandler();
    }

    /**
     *
     * Validiert die XML.
     *
     *
     * @param lXml
     * @return isValid
     * @throws SAXException Fehler beim Parsen.
     * @throws IOException String nicht vorhanden.
     * @throws ParserConfigurationException Fehler beim Parsen.
     */
    public boolean isValid(String lXml) throws SAXException, IOException, ParserConfigurationException {


        SAXParser parser = null;
        parser = SAXParserFactory.newInstance().newSAXParser();
        Reader stringReader = new StringReader(lXml);
        parser.parse(new InputSource(stringReader), handler);

        return valid;
    }

    /**
     * Beim Parsen der XML werden hier die Methoden aufgerufen, sobald ein Error
     * auftritt, ein Element startet oder endet.
     */
    private class ToolDocumentHandler extends DefaultHandler {

        /**
         * Bei einem Error beim Parsen der XML wird diese Methode aufgerufen.
         *
         * @param e
         * @throws SAXException
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            super.error(e);
            valid = false;
        }

        /**
         * Ein fatalError tritt auf, sobald die XML nicht mehr geparst werden
         * kann.
         *
         * @param e
         * @throws SAXException
         */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            super.fatalError(e);
            valid = false;
        }

        /**
         * Sobald ein Element startet, beim Parsen der XML, wird diese Methode
         * aufgerufen.
         *
         * @param uri Uri
         * @param localName Name des Tags
         * @param qName qualified Name des Tags
         * @param attributes Attribute des Tags.
         * @throws SAXException Fehler beim Parsen der XML.
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);

            if (nodesStart) {

                if (qName.equals("node")) {

                    if (attributes.getValue("type").equals("Conveyor.MGX.GetMGXJob")) {
                        valid = true;
                        nodesStart = false;
                    } else {
                        valid = false;
                        nodesStart = false;
                    }
                } else {
                    valid = false;
                    nodesStart = false;
                }

            }
            if (qName.equals("nodes")) {
                nodesStart = true;
            } else {
                nodesStart = false;
            }
        }
    }
}
