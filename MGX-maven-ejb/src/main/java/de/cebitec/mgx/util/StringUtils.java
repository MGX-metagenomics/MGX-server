package de.cebitec.mgx.util;

import java.util.Iterator;

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
}
