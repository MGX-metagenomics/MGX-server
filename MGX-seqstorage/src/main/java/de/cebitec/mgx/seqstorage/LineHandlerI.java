/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.cebitec.mgx.seqstorage;

/**
 *
 * @author sjaenick
 */
public interface LineHandlerI {

    public void handle(byte[] buf, int start, int end);
}
