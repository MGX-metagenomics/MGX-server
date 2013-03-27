package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.seqholder.DNAQualitySequenceHolder;
import de.cebitec.mgx.seqholder.ReadSequenceI;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * based on code by
 *
 * @author Charles Imbusch < charles at imbusch dot com >
 * http://mirror.transact.net.au/pub/sourceforge/h/project/ho/homology-guided/sffParser/src/
 */
public class SFFReader implements SeqReaderI<ReadSequenceI> {

    private long offset = 0;                    // should always point to begining of read header
    private RandomAccessFile raf;
    private TObjectLongMap<String> reads = new TObjectLongHashMap<>();      // hold indentifiers and filepointers
    // Common Header variables
    private int magicNumber;
    private String version;
    private long indexOffset;
    private int indexLength;
    private int numberOfReads;
    private int headerLength;
    private int keyLength;
    private int numberOfFlowsPerRead;
    private String FlowgramFormatCode;
    private String flowChars;
    private String keySequence;
    private int indexMagicNumber;
    private String indexVersion;

    /**
     * Constructor for one single sff file. Shouldn't be used by a user. Need to think about that???
     *
     * @param filename
     * @throws FileNotFoundException
     * @throws IOException
     */
    public SFFReader(String filename) {
        try {
            raf = new RandomAccessFile(filename, "r");
            this.readCommonHeader();
            this.createIndex();
            this.useIndex();
        } catch (IOException ex) {
            Logger.getLogger(SFFReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param identifier
     * @return DNAQualitySequenceI object
     * @throws IOException
     */
    private DNAQualitySequenceI getRead(String identifier) throws IOException {
        // lookup position for read
        offset = reads.get(identifier);
        // set raf to that position
        raf.seek(offset);
        // Read Header Section
        // readHeaderLength
        byte[] readHeaderLengthByte = new byte[2];
        raf.read(readHeaderLengthByte);
//        BigInteger readHeaderLengthBig = new BigInteger(readHeaderLengthByte);
//        int readHeaderLength = readHeaderLengthBig.intValue();
        // nameLength
        byte[] nameLengthByte = new byte[2];
        raf.read(nameLengthByte);
        BigInteger nameLengthBig = new BigInteger(nameLengthByte);
//        int nameLength = nameLengthBig.intValue();
        // numberOfBases
        byte[] numberOfBasesByte = new byte[4];
        raf.read(numberOfBasesByte);
        BigInteger numberOfBasesBig = new BigInteger(numberOfBasesByte);
//        int numberOfBases = numberOfBasesBig.intValue();
        // clipQualLeft
        byte[] clipQualLeftByte = new byte[2];
        raf.read(clipQualLeftByte);
        BigInteger clipQualLeftBig = new BigInteger(clipQualLeftByte);
        int clipQualLeft = clipQualLeftBig.intValue();
        // clipQualRight
        byte[] clipQualRightByte = new byte[2];
        raf.read(clipQualRightByte);
        BigInteger clipQualRightBig = new BigInteger(clipQualRightByte);
        int clipQualRight = clipQualRightBig.intValue();
        // clipAdapterLeft
        byte[] clipAdapterLeftByte = new byte[2];
        raf.read(clipAdapterLeftByte);
        BigInteger clipAdapterLeftBig = new BigInteger(clipAdapterLeftByte);
        int clipAdapterLeft = clipAdapterLeftBig.intValue();
        // clipAdapterRight
        byte[] clipAdapterRightByte = new byte[2];
        raf.read(clipAdapterRightByte);
        BigInteger clipAdapterRightBig = new BigInteger(clipAdapterRightByte);
        int clipAdapterRight = clipAdapterRightBig.intValue();
        // name
        byte[] nameByte = new byte[nameLengthBig.intValue()];
        raf.read(nameByte);
        String name = new String(nameByte);
        // Read Data Section
        raf.seek(eightBytePadding(raf.getFilePointer()));
        // flowgramValues
//        float flowgramValues[] = new float[this.getNumberOfFlowsPerRead()];
        byte[] flowgramValuesByte = new byte[this.getNumberOfFlowsPerRead() * 2];
        raf.read(flowgramValuesByte);
//        int j = 0; // variable counting in flowgram values
//        for (int i = 0; i < flowgramValuesByte.length;) {
//            // get one flowgram value with 2 bytes
//            byte[] flowgramValue = new byte[2];
//            flowgramValue[0] = flowgramValuesByte[i];
//            flowgramValue[1] = flowgramValuesByte[i + 1];
//            BigInteger flowgramValueBig = new BigInteger(flowgramValue);
//            flowgramValues[j] = flowgramValueBig.floatValue() * 1 / 100;
//            i = i + 2;
//            j++;
//        }
        // flowIndexPerBase
//        int flowIndexPerBase[] = new int[numberOfBasesBig.intValue()];
//        byte[] flowIndexPerBaseByte = new byte[numberOfBasesBig.intValue()];
//        raf.read(flowIndexPerBaseByte);
//        for (int i = 0; i < flowIndexPerBaseByte.length; i++) {
//            flowIndexPerBase[i] = Integer.valueOf((flowIndexPerBaseByte[i] + "")).intValue();
//        }
        // bases
        byte[] basesByte = new byte[numberOfBasesBig.intValue()];
        raf.read(basesByte);
        //       String bases = new String(basesByte);
        // qualityScores
        //       int qualityScores[] = new int[numberOfBasesBig.intValue()];
        byte[] qualityScoresByte = new byte[numberOfBasesBig.intValue()];
        raf.read(qualityScoresByte);
//        for (int i = 0; i < qualityScoresByte.length; i++) {
//            qualityScores[i] = Integer.valueOf((qualityScoresByte[i] + "")).intValue();
//        }

        DNAQualitySequenceI dnasequence = new QualityDNASequence();
        dnasequence.setName(name.getBytes());

        int left = Math.max(clipAdapterLeft, clipQualLeft);
        int right = clipAdapterRight == 0 ? clipQualRight : Math.min(clipAdapterRight, clipQualRight);
        basesByte = Arrays.copyOfRange(basesByte, left - 1, right);
        assert basesByte.length == qualityScoresByte.length;
        dnasequence.setSequence(basesByte);
        dnasequence.setQuality(qualityScoresByte);
//        ArrayList noteslist = new ArrayList(7);
//        noteslist.add(clipQualLeft);
//        noteslist.add(clipQualRight);
//        noteslist.add(clipAdapterLeft);
//        noteslist.add(clipAdapterRight);
//        noteslist.add(flowgramValues);
//        noteslist.add(flowIndexPerBase);
//        noteslist.add(qualityScores);
//        dnasequence.setNotesList(noteslist);
        return dnasequence;
    }

    /**
     * Reads the common header of the sff file.
     *
     * @throws IOException
     */
    private void readCommonHeader() throws IOException {
        // magicNumber
        byte[] magicNumberByte = new byte[4];
        raf.read(magicNumberByte);
        BigInteger magicNumberBig = new BigInteger(magicNumberByte);
        magicNumber = magicNumberBig.intValue();
        checkMagicNumber(magicNumberByte);
        // version
        byte[] versionByte = new byte[4];
        raf.read(versionByte);
        version = new String(versionByte);
        // indexOffset
        byte[] indexOffsetByte = new byte[8];
        raf.read(indexOffsetByte);
        BigInteger indexOffsetBig = new BigInteger(indexOffsetByte);
        indexOffset = indexOffsetBig.longValue();
        // indexLength
        byte[] indexLengthByte = new byte[4];
        raf.read(indexLengthByte);
        BigInteger indexLengthBig = new BigInteger(indexLengthByte);
        indexLength = indexLengthBig.intValue();
        // numberOfReads
        byte[] numberOfReadsByte = new byte[4];
        raf.read(numberOfReadsByte);
        BigInteger numberOfReadsBig = new BigInteger(numberOfReadsByte);
        numberOfReads = numberOfReadsBig.intValue();
        // headerLength
        byte[] headerLengthByte = new byte[2];
        raf.read(headerLengthByte);
        BigInteger headerLengthBig = new BigInteger(headerLengthByte);
        headerLength = headerLengthBig.intValue();
        // keyLength
        byte[] keyLengthByte = new byte[2];
        raf.read(keyLengthByte);
        BigInteger keyLengthBig = new BigInteger(keyLengthByte);
        keyLength = keyLengthBig.intValue();
        // numberOfFlowsPerRead
        byte[] numberOfFlowsPerReadByte = new byte[2];
        raf.read(numberOfFlowsPerReadByte);
        BigInteger numberOfFlowsPerReadBig = new BigInteger(numberOfFlowsPerReadByte);
        numberOfFlowsPerRead = numberOfFlowsPerReadBig.intValue();
        // FlowgramFormatCode
        byte[] flowgramFormatCodeByte = new byte[1];
        raf.read(flowgramFormatCodeByte);
        FlowgramFormatCode = new String(flowgramFormatCodeByte);
        // flowChars
        byte[] flowCharsByte = new byte[numberOfFlowsPerReadBig.intValue()];
        raf.read(flowCharsByte);
        flowChars = new String(flowCharsByte);
        // keySequence
        byte[] keySequenceByte = new byte[keyLengthBig.intValue()];
        raf.read(keySequenceByte);
        keySequence = new String(keySequenceByte);
        // go to index location
        raf.seek(indexOffsetBig.longValue());
        // indexMagicNumber
        byte[] indexMagicNumberByte = new byte[4];
        raf.read(indexMagicNumberByte);
        BigInteger indexMagicNumberBig = new BigInteger(indexMagicNumberByte);
        indexMagicNumber = indexMagicNumberBig.intValue();
        // indexVersion
        byte[] indexVersionByte = new byte[4];
        raf.read(indexVersionByte);
        indexVersion = new String(indexVersionByte);
    }

    /**
     * Creates an index of the sff file by scanning the file.
     *
     * @throws IOException
     */
    private void createIndex() throws IOException {
        // go to first read
        offset = eightBytePadding(headerLength);
        while (offset < indexOffset) {
            raf.seek(offset);
            // readHeaderLength
            byte[] readHeaderLength = new byte[2];
            raf.read(readHeaderLength);
            BigInteger readHeaderLengthBig = new BigInteger(readHeaderLength);
            // nameLength
            byte[] nameLength = new byte[2];
            raf.read(nameLength);
            BigInteger nameLengthBig = new BigInteger(nameLength);
            // numberOfBasesBig
            byte[] numberOfBases = new byte[4];
            raf.read(numberOfBases);
            BigInteger numberOfBasesBig = new BigInteger(numberOfBases);
            // skip to name of read
            raf.seek(raf.getFilePointer() + 8);
            // name
            byte[] name = new byte[nameLengthBig.intValue()];
            raf.read(name);
            // add read to hashtable
            reads.put(new String(name), offset);
            // calculate readDataLength
            long readDataLength = numberOfFlowsPerRead * 2 + 3 * numberOfBasesBig.longValue();
            // calculate offset for next read
            offset = eightBytePadding(eightBytePadding(raf.getFilePointer()) + readDataLength);
        }
    }

    /**
     * @param offset
     * @return Next offset to read data from.
     */
    private static long eightBytePadding(long offset) {
        int align = 8;
        offset = offset + ((align - (offset % align)) % align);
        return offset;
    }

    /**
     * Checks if file is a sff file.
     *
     * @param magic_number
     */
    private static void checkMagicNumber(byte[] magicNumber) throws IOException {
        if (!new String(magicNumber).equals(".sff")) {
            throw new IOException("Argument is not an sff file!");
        }
    }

    /**
     * @return magicNumber
     */
    public int getMagicNumber() {
        return magicNumber;
    }

    /**
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return indexOffset
     */
    public long getIndexOffset() {
        return indexOffset;
    }

    /**
     * @return indexLength
     */
    public int getIndexLength() {
        return indexLength;
    }

    /**
     * @return numberOfReads
     */
    public int getNumberOfReads() {
        return numberOfReads;
    }

    /**
     * @return headerLength
     */
    public int getHeaderLength() {
        return headerLength;
    }

    /**
     * @return keyLength
     */
    public int getKeyLength() {
        return keyLength;
    }

    /**
     * @return numberOfFlowsPerRead
     */
    public int getNumberOfFlowsPerRead() {
        return numberOfFlowsPerRead;
    }

    /**
     * @return FlowgramFormatCode
     */
    public String getFlowgramFormatCode() {
        return FlowgramFormatCode;
    }

    /**
     * @return flowChars
     */
    public String getFlowChars() {
        return flowChars;
    }

    /**
     * @return keySequence
     */
    public String getKeySequence() {
        return keySequence;
    }

    /**
     * @return indexMagicNumber
     */
    public int getIndexMagicNumber() {
        return indexMagicNumber;
    }

    /**
     * @return indexVersion
     */
    public String getIndexVersion() {
        return indexVersion;
    }

    /**
     * testmethod to make use of mft index structur.
     */
    private void useIndex() throws IOException {
        raf.seek(indexOffset);

        byte[] first = new byte[8];
        raf.read(first);
        //System.out.println(new String(first));

        // XMLManifestLength
        byte[] XMLManifestLengthByte = new byte[4];
        raf.read(XMLManifestLengthByte);
        BigInteger XMLManifestLengthBig = new BigInteger(XMLManifestLengthByte);
        int XMLManifestLength = XMLManifestLengthBig.intValue();
        //
        byte[] IndexDataLengthByte = new byte[4];
        raf.read(IndexDataLengthByte);
        BigInteger IndexDataLengthBig = new BigInteger(IndexDataLengthByte);
        int IndexDataLength = IndexDataLengthBig.intValue();

        byte[] XMLManifest = new byte[XMLManifestLength];
        raf.read(XMLManifest);


    }

    @Override
    public void delete() {
    }

    @Override
    public Set<ReadSequenceI> fetch(long[] ids) throws SeqStoreException {
        return null;
    }
    private Iterator<String> iter = null;

    @Override
    public boolean hasMoreElements() {
        if (iter == null) {
            iter = reads.keySet().iterator();
        }
        return iter.hasNext();
    }

    @Override
    public ReadSequenceI nextElement() {
        assert iter != null;
        try {
            return new DNAQualitySequenceHolder(getRead(iter.next()));
        } catch (IOException ex) {
            Logger.getLogger(SFFReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        assert false;
        return null;
    }

    @Override
    public void close() throws Exception {
        if (raf != null) {
            raf.close();
        }
    }
}
