package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class FASTQReader implements SeqReaderI {

    private DNASequenceI seq = null;
    private ByteStreamTokenizer stream = null;
    private String fastqfile = null;
    public static final byte LINEBREAK = '\n';

    public FASTQReader(String filename) throws SeqStoreException {
        fastqfile = filename;
        try {
            stream = new ByteStreamTokenizer(fastqfile, LINEBREAK, 0);
        } catch (Exception ex) {
            throw new SeqStoreException("File not found or unreadable: " + fastqfile);
        }
    }

    @Override
    public boolean hasMoreElements() {

        byte[] l1, l2, l3, l4;

        try {
            l1 = stream.hasMoreElements() ? stream.nextElement() : null; // header
            l2 = stream.hasMoreElements() ? stream.nextElement() : null; // dna sequence
            l3 = stream.hasMoreElements() ? stream.nextElement() : null; // quality header
            l4 = stream.hasMoreElements() ? stream.nextElement() : null; // quality string
        } catch (Exception e) {
            return false;
        }

        if ((l1 == null) || (l2 == null)) {
            return false;
        }

        // remove leading '@' from sequence name
        byte[] seqname = new byte[l1.length - 1];
        System.arraycopy(l1, 1, seqname, 0, l1.length - 1);

        seq = new DNASequence();
        seq.setName(seqname);
        seq.setSequence(l2);

        return true;
    }

    @Override
    public DNASequenceI nextElement() {
        return seq;
    }

    @Override
    public void close() {
        if (stream != null) {
            stream.close();
        }
    }

    @Override
    public void delete() {
        close();
        File f = new File(fastqfile);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
    }

    @Override
    public Set<DNASequenceI> fetch(Set<Long> ids) throws SeqStoreException {
        Set<DNASequenceI> res = new HashSet<DNASequenceI>(ids.size());
        while (hasMoreElements() && !ids.isEmpty()) {
            DNASequenceI elem = nextElement();
            if (ids.contains(elem.getId())) {
                res.add(elem);
                ids.remove(elem.getId());
            }
        }

        if (!ids.isEmpty()) {
            throw new SeqStoreException("Could not retrieve all sequences.");
        }
        return res;
    }
}
