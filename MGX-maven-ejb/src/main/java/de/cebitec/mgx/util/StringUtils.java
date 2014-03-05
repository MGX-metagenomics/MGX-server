package de.cebitec.mgx.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author sjaenick
 */
public class StringUtils {
    /*
     * from http://snippets.dzone.com/posts/show/91
     */

    public static String join(Iterable< ? extends Object> pColl, String separator) {
        Iterator< ? extends Object> oIter;
        if (pColl == null || (!(oIter = pColl.iterator()).hasNext())) {
            return "";
        }
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext()) {
            oBuilder.append(separator).append(oIter.next());
        }
        return oBuilder.toString();
    }
    
    
    public static List<String> split(String message, String separator) {
        return new ArrayList<>(Arrays.asList(message.split(separator)));
    }
}
