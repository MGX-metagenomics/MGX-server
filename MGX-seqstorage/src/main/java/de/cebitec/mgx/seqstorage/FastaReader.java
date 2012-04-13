package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.seqstorage.encoding.ByteUtils;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class FastaReader implements SeqReaderI {

    private ByteStreamTokenizer stream = null;
    private byte[] buf = null;
    private DNASequenceI seq = null;
    private String fastafile = null;
    public static final byte LINEBREAK = '\n';

    public FastaReader(String filename) throws SeqStoreException {
        fastafile = filename;
        try {
            stream = new ByteStreamTokenizer(fastafile, LINEBREAK, 0);
        } catch (Exception ex) {
            throw new SeqStoreException("File not found or unreadable: " + fastafile);
        }

        // read first line
        buf = stream.hasMoreElements() ? stream.nextElement() : null; // header
    }

    @Override
    public boolean hasMoreElements() {
        if (stream == null) {
            return false;
        }
        
        if (buf.length == 0) {
            while (buf.length == 0 && stream.hasMoreElements()) {
                buf = stream.nextElement();
            }
        }
        
        // sequence header has to start with '>'
        if (buf.length == 0 || buf[0] != '>') {
            return false;
        }

        // process sequence header
        byte[] seqname = new byte[buf.length - 1];
        System.arraycopy(buf, 1, seqname, 0, buf.length - 1);
        
        // check sequence name for whitespaces and trim
        int trimPos = 0;
        for (int i=0; i< seqname.length; i++) {
            if (seqname[i] == ' ') {
                trimPos = i;
                break;
            }
        }
        if (trimPos > 0) {
            System.arraycopy(seqname, 0, seqname, 0, trimPos);
        }
        
        seq = new DNASequence();
        seq.setName(seqname);

        byte[] dnasequence = null;

        while (stream.hasMoreElements()) {
            buf = stream.nextElement();
            
            if (buf.length > 0 && buf[0] == '>') {
                // we have reached the next sequence
                seq.setSequence(dnasequence);
                return true;
            }

            if (dnasequence == null) {
                // start new sequence
                dnasequence = new byte[buf.length];
                System.arraycopy(buf, 0, dnasequence, 0, buf.length);
            } else {
                dnasequence = ByteUtils.concat(dnasequence, buf);
            }
        }
        seq.setSequence(dnasequence);
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
        File f = new File(fastafile);
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
