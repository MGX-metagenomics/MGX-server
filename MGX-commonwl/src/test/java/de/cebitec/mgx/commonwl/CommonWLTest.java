/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.commonwl;

import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
public class CommonWLTest {

    public CommonWLTest() {
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

    /**
     * Test of getParameters method, of class CommonWL.
     */
    @Test
    public void testGetParameters() {
        System.out.println("getParameters");

        File f = new File("src/main/resources/test.cwl");

        if (!f.exists() && f.canRead()) {
            fail("File " + f.getAbsolutePath() + " missing or unreadable.");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        String workflowContent = sb.toString();
        AutoCloseableIterator<JobParameter> result = CommonWL.getParameters(workflowContent);
        int cnt = 0;
        while (result.hasNext()) {
            JobParameter jp = result.next();
            System.out.println(jp.getUserName());
            System.out.println(jp.getUserDescription());
            System.out.println(jp.getDisplayName());
            cnt++;
        }
        assertEquals(1, cnt);
    }

}
