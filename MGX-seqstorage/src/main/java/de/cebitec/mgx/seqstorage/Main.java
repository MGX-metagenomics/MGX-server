package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.sequence.DNASequenceI;
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
        FastaReader fr = new FastaReader("/homes/sjaenick/CSF/in.fas");
        CSFWriter csf = new CSFWriter("/homes/sjaenick/CSF/1");
        long curId = 1;
        while (fr.hasMoreElements()) {
            DNASequenceI seq = fr.nextElement().getSequence();
            seq.setId(curId++);
            csf.addSequence(seq);
        }
        csf.close();
        fr.close();
        
        CSFReader reader = new CSFReader("/homes/sjaenick/CSF/1");
        long ids[] = new long[]{400};
        Set<DNASequenceHolder> reads = reader.fetch(ids);
        for (DNASequenceHolder h : reads) {
            DNASequenceI seq = h.getSequence();
            System.out.println(">" + seq.getId());
            System.out.println(new String(seq.getSequence()));
        }
        
        reader.close();

    }
}