/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.statistics;

import java.util.Random;

/**
 *
 * @author sj
 */
public class Util {

    static String generateSuffix(String prefix) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder(prefix);
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    static boolean contains(String[] options, String value) {
        for (String o : options) {
            if (o.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private Util() {
    }

}
