/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.web.helper;

import de.cebitec.mgx.controller.MGXException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public class Util {

    public static String readFile(File f) throws MGXException {
        if (!f.exists() && f.canRead()) {
            throw new MGXException("Job definition file missing or unreadable.");
        }

        StringBuilder sb = new StringBuilder();
        try {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ex) {
            throw new MGXException(ex);
        }
        return sb.toString();
    }
}
