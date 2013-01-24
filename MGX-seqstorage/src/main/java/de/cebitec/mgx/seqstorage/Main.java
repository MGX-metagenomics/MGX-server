package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SeqStoreException, IOException {
        CSFReader csf = new CSFReader("/vol/mgx-data/MGX_Biofilter/seqruns/1");
        Set<DNASequenceHolder> fetch = csf.fetch(new long[]{50,239});
        for (DNASequenceHolder h : fetch) {
            System.err.println(h.getSequence().getId());
        }


        fetch = csf.fetch(new long[]{50});
        for (DNASequenceHolder h : fetch) {
            System.err.println(h.getSequence().getId());
        }

        csf.close();

    }
}