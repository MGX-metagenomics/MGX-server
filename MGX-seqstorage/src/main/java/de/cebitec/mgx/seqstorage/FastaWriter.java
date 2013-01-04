package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqStoreException;
import de.cebitec.mgx.sequence.SeqWriterI;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public class FastaWriter implements SeqWriterI<DNASequenceI> {

    private BufferedWriter seqout;

    public FastaWriter(String filename) throws IOException, SeqStoreException {
        File f = new File(filename);
        if (f.exists()) {
            throw new SeqStoreException("File "+filename+" already exists.");
        }
        seqout = new BufferedWriter(new FileWriter(filename)); 
    }

    @Override
    public void addSequence(DNASequenceI seq) throws IOException {
        StringBuilder sb = new StringBuilder(">");
        sb.append(new String(seq.getName()));
        sb.append("\n");
        sb.append(new String(seq.getSequence()));
        sb.append("\n");
        seqout.write(sb.toString());
    }

    @Override
    public void close() throws IOException {
        if (seqout != null) {
            seqout.close();
        }
    }
}
