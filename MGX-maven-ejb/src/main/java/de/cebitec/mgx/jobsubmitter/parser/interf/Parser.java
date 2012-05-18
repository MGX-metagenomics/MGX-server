
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package de.cebitec.mgx.jobsubmitter.parser.interf;

//~--- JDK imports ------------------------------------------------------------

import de.cebitec.mgx.jobsubmitter.data.interf.NodeStore;






/**
 *
 * @author belmann
 */
public interface Parser {
   
   /**
    * Gibt die konfigurierbaren Nodes mit ihren ConfigItems wieder.
    * @param toolXml XML Datei mit den vom User zusammengestellten Tools.
    * @param pluginsXml Beinhaltet alle möglichen Nodes.
    * @return NodeStore mit allen konfigurierbaren Knoten.
    */
   public NodeStore getNodesConfigurations(String toolXml,
         String pluginsXml);
}


//~ Formatted by Jindent --- http://www.jindent.com
