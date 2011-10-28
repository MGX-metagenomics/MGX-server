package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.seqstorage.encoding.ByteUtils;
import de.cebitec.mgx.seqstorage.encoding.FourBitEncoder;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class CSFReader implements SeqReaderI {

    private ByteStreamTokenizer seqin;
    private InputStream namein;
    private DNASequenceI seq = null;
    private String csffile = null;
    private String namefile = null;

    public CSFReader(String filename) throws SeqStoreException {
        csffile = filename + ".csf";
        namefile = filename;
        try {
            validateMagic(namefile, FourBitEncoder.NMS_MAGIC);
            validateMagic(csffile, FourBitEncoder.CSF_MAGIC);
            seqin = new ByteStreamTokenizer(csffile, FourBitEncoder.RECORD_SEPARATOR, FourBitEncoder.CSF_MAGIC.length);
            namein = new BufferedInputStream(new FileInputStream(namefile));
            namein.skip(FourBitEncoder.CSF_MAGIC.length);
        } catch (Exception ex) {
            throw new SeqStoreException(ex.getMessage());
        }
    }

    @Override
    public boolean hasMoreElements() {
        // extract substring of element, removing last 8bytes (offset)
        byte[] record = new byte[16];
        byte[] seqId = new byte[8];

        try {
            if (namein.read(record) == -1) {
                return false;
            }
        } catch (IOException ex) {
            seqId = null;
        }

        // extract sequence id and convert
        System.arraycopy(record, 0, seqId, 0, 8);
        long sequence_id = ByteUtils.bytesToLong(seqId);

        if (!seqin.hasMoreElements()) {
            return false;
        }

        byte[] dnasequence = seqin.nextElement();

        if ((seqId != null) && (dnasequence != null)) {
            seq = new DNASequence(sequence_id);
            seq.setSequence(FourBitEncoder.decode(dnasequence));
            return true;
        }
        return false;

    }

    @Override
    public DNASequenceI nextElement() {
        return seq;
    }

    @Override
    public void close() {
        if (seqin != null) {
            seqin.close();
        }
        if (namein != null) {
            try {
                namein.close();
            } catch (IOException ex) {
            }
        }
    }

    private void validateMagic(String filename, byte[] magic) throws SeqStoreException {
        // validate magic
        try {
            FileInputStream fis = new FileInputStream(filename);
            byte[] tmp = new byte[4];
            fis.read(tmp, 0, 4);
            if (!Arrays.equals(magic, tmp)) {
                throw new SeqStoreException(filename + ": Invalid magic: " + new String(tmp));
            }
            fis.close();
        } catch (Exception e) {
            throw new SeqStoreException(filename + ": Invalid magic");
        }
    }

    @Override
    public void delete() {
        close();
        File f = new File(csffile);
        File g = new File(namefile);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
        if (g.exists() && g.isFile()) {
            g.delete();
        }
    }

    @Override
    public Set<DNASequenceI> fetch(Set<Long> ids) throws SeqStoreException {
        Set<DNASequenceI> res = new HashSet<DNASequenceI>(ids.size());
        
        // FIXME: use the .nms index & sort offsets instead of iterating
        // over all sequences
        
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
