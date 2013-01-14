package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.seqholder.DNASequenceHolder;
import de.cebitec.mgx.seqstorage.encoding.ByteUtils;
import de.cebitec.mgx.seqstorage.encoding.FourBitEncoder;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
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
public class CSFReader implements SeqReaderI<DNASequenceHolder> {

    private ByteStreamTokenizer seqin;
    private InputStream namein;
    private final String csffile;
    private final String namefile;
    private DNASequenceHolder holder = null;

    public CSFReader(String filename) throws SeqStoreException {
        csffile = filename + ".csf";
        namefile = filename;
        try {
            validateMagic(namefile, FourBitEncoder.NMS_MAGIC);
            validateMagic(csffile, FourBitEncoder.CSF_MAGIC);
            seqin = new ByteStreamTokenizer(csffile, FourBitEncoder.RECORD_SEPARATOR, FourBitEncoder.CSF_MAGIC.length);
            namein = new BufferedInputStream(new FileInputStream(namefile));
            if (namein.skip(FourBitEncoder.CSF_MAGIC.length) < FourBitEncoder.CSF_MAGIC.length) {
                throw new SeqStoreException("Corrupted file " + csffile);
            }
        } catch (SeqStoreException | IOException ex) {
            throw new SeqStoreException(ex.getMessage());
        }
    }

    @Override
    public boolean hasMoreElements() {

        if (holder != null) {
            // element in holder not yet retrieved
            return true;
        }
        
        /*
         * read new element
         */

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
            DNASequenceI seq = new DNASequence(sequence_id);
            seq.setSequence(FourBitEncoder.decode(dnasequence));

            holder = new DNASequenceHolder(seq);
            return true;
        }
        return false;
    }

    @Override
    public DNASequenceHolder nextElement() {
        assert holder != null;
        DNASequenceHolder ret = holder;
        holder = null;
        return ret;
    }

    @Override
    public void close() {
        if (seqin != null) {
            seqin.close();
            seqin = null;
        }
        if (namein != null) {
            try {
                namein.close();
            } catch (IOException ex) {
            }
            namein = null;
        }
    }

    private void validateMagic(String filename, final byte[] magic) throws SeqStoreException {
        // validate magic
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            byte[] tmp = new byte[magic.length];
            if (fis.read(tmp, 0, magic.length) < magic.length) {
                throw new SeqStoreException("Truncated file " + filename + "?");
            };
            if (!Arrays.equals(magic, tmp)) {
                throw new SeqStoreException(filename + ": Invalid magic: " + new String(tmp));
            }
        } catch (IOException e) {
            throw new SeqStoreException(filename + ": Invalid magic");
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                throw new SeqStoreException(ex.getMessage());
            }
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
    public Set<DNASequenceHolder> fetch(long[] ids) throws SeqStoreException {
        Set<DNASequenceHolder> result = new HashSet<>(ids.length);
        if (ids.length == 0) {
            return result;
        }
        Arrays.sort(ids);

        NMSReader idx;
        InputStream in;
        try {
            idx = new NMSReader(namein, ids);
            in = new BufferedInputStream(new FileInputStream(csffile));
            assert in.markSupported();
            in.mark(Integer.MAX_VALUE);
        } catch (IOException ex) {
            throw new SeqStoreException("Could not parse index.");
        }

        try {
            byte[] buf = new byte[200];
            int bytesRead = 0;
            for (long id : ids) {
                long offset = idx.getOffset(id);
                if (offset == -1) {
                    throw new SeqStoreException("Sequence ID " + id + " not present in index.");
                }
                in.reset();
                in.skip(offset);
                bytesRead = in.read(buf);
                while (-1 == getSeparatorPos(buf, FourBitEncoder.RECORD_SEPARATOR) && bytesRead != -1) {
                    System.err.println("reading more..");
                    byte newbuf[] = new byte[buf.length * 2];
                    System.arraycopy(buf, 0, newbuf, 0, buf.length);
                    bytesRead = in.read(newbuf, buf.length, buf.length);
                    buf = newbuf;
                }
                int sepPos = getSeparatorPos(buf, FourBitEncoder.RECORD_SEPARATOR);
                byte[] encoded = ByteUtils.substring(buf, 0, sepPos - 1);

                DNASequenceI seq = new DNASequence(id);
                seq.setSequence(FourBitEncoder.decode(encoded));
                result.add(new DNASequenceHolder(seq));
            }
        } catch (IOException ex) {
            throw new SeqStoreException("Internal error.");
        }

        if (result.size() != ids.length) {
            throw new SeqStoreException("Could not retrieve all sequences.");
        }
        return result;
    }

    private int getSeparatorPos(byte[] in, byte separator) {
        for (int i = 0; i <= in.length - 1; i++) {
            if (in[i] == separator) {
                return i;
            }
        }
        return -1;
    }

    private class NMSReader {

        final TLongLongMap idx;
        final InputStream nmsStream;
        final long[] ids;

        public NMSReader(InputStream nmsStream, long[] ids) throws IOException {
            this.idx = new TLongLongHashMap(ids.length, 1.0F, -1, -1);
            this.nmsStream = nmsStream;
            this.ids = ids;
            readRequired();
        }

        public long getOffset(long id) {
            return idx.get(id);
        }

        private void readRequired() throws IOException {
            if (ids.length == 0) {
                return;
            }
            long max = max(ids);
            byte[] buf = new byte[16];
            while (16 == nmsStream.read(buf)) {
                long id = ByteUtils.bytesToLong(ByteUtils.substring(buf, 0, 7));
                long offset = ByteUtils.bytesToLong(ByteUtils.substring(buf, 8, 15));
                idx.put(id, offset);

                if (max == id) {
                    nmsStream.close();
                    return;
                }
            }
            nmsStream.close();
        }

        private long max(long[] values) {
            long max = values[0];
            for (long value : values) {
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }
    }
}
