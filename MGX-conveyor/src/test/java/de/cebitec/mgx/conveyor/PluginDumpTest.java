/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author sj
 */
public class PluginDumpTest {

    public PluginDumpTest() {
    }

    @Test
    public void testParseDump() throws Exception {
        File plugindump = new File("src/test/resources/plugindump.xml");
        PluginDump pd = new PluginDump(plugindump);
        try {
            pd.parse();
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            Logger.getLogger(PluginDumpTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(460, pd.size());

//        String toolXml = UnixHelper.readFile(new File("src/test/resources/qiime_assignTaxonomy.xml"));
//        ConveyorWorkflow workFlow = new ConveyorWorkflow(toolXml, pd);
//        workFlow.parse();
    }

    @Test
    public void testParseDumpWithBlackList() throws Exception {
        File plugindump = new File("src/test/resources/plugindump.xml");

        PluginDump pdComplete = new PluginDump(plugindump);
        
        Collection<String> filter = new ArrayList<>(1);
        filter.add("Conveyor.Tree.IsLeafVertex`1");
        PluginDump pd = new PluginDump(plugindump, filter);
        try {
            pdComplete.parse();
            pd.parse();
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            fail(ex.getMessage());
        }
        assertEquals(pdComplete.size() - 1, pd.size());

    }

}
