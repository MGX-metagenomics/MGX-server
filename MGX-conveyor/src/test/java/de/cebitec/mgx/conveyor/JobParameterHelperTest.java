/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.UnixHelper;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sj
 */
public class JobParameterHelperTest {

    public JobParameterHelperTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetParameters() throws IOException {
        System.out.println("getParameters");
        String toolXml = UnixHelper.readFile(new File("src/test/resources/qiime_assignTaxonomy.xml"));
        File plugindump = new File("src/test/resources/plugindump.xml");
        AutoCloseableIterator<JobParameter> iter = null;
        try {
            iter = JobParameterHelper.getParameters(toolXml, plugindump);
        } catch (MGXException ex) {
            Logger.getLogger(JobParameterHelperTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertNotNull(iter);
        JobParameter jp = null;
        int cnt = 0;
        while (iter.hasNext()) {
            jp = iter.next();
            assertNotNull(jp);
            cnt++;
            if (jp.getParameterName().equals("database")) {
                assertEquals(2, jp.getChoices().size());
            }
        }
        assertEquals(4, cnt);
    }

    @Test
    public void testRegressionNullType() throws IOException {
        System.out.println("testRegressionNullType");
        String toolXml = UnixHelper.readFile(new File("src/test/resources/blastmapping.xml"));
        File plugindump = new File("src/test/resources/plugindump.xml");
        AutoCloseableIterator<JobParameter> iter = null;
        try {
            iter = JobParameterHelper.getParameters(toolXml, plugindump);
        } catch (MGXException ex) {
            Logger.getLogger(JobParameterHelperTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertNotNull(iter);
        JobParameter jp;
        int cnt = 0;
        while (iter.hasNext()) {
            jp = iter.next();
            assertNotNull(jp);
//            System.err.println(jp);
            assertNotNull(jp.getType());
            cnt++;
        }
        assertEquals(3, cnt);
    }

}
