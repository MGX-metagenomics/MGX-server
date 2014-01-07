/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.util;

import java.util.logging.Logger;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 *
 * @author belmann
 */
public class AutoCloseableSAMRecordIterator implements AutoCloseableIterator<MappedSequence> {

    SAMRecordIterator iterator;
    int counter;
    
    public AutoCloseableSAMRecordIterator(SAMRecordIterator iterator) {
        this.iterator = iterator;
        counter = -1;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public MappedSequence next() {
       SAMRecord record =  this.iterator.next();
       counter++;
       return new MappedSequence(counter, record.getAlignmentStart(), record.getAlignmentEnd(),0);
    }

    @Override
    public void remove() {
    }

    @Override
    public void close() throws Exception {
     //nothing
    }
}
