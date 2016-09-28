/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.JobParameter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author sj
 */
public class Conveyor {

    private PluginDump pluginDump = null;

    public Collection<JobParameter> getParameters(String toolXML, File pluginDumpFile) throws MGXException {
        if (pluginDump == null || pluginDumpFile.lastModified() > pluginDump.lastModified()) {
            pluginDump = new PluginDump(pluginDumpFile);
            try {
                pluginDump.parse();
            } catch (SAXException | ParserConfigurationException | IOException ex) {
                pluginDump = null;
                throw new MGXException(ex);
            }
        }

        ConveyorWorkflow workFlow = new ConveyorWorkflow(toolXML, pluginDump);
        return workFlow.getParameters();
    }
}
