package de.cebitec.mgx.seqstorage;

import de.cebitec.mgx.sequence.FactoryI;
import de.cebitec.mgx.sequence.SeqReaderI;
import de.cebitec.mgx.sequence.SeqStoreException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public class ReaderFactory implements FactoryI {

    public ReaderFactory() {
    }

    @Override
    public SeqReaderI getReader(String uri) throws SeqStoreException {
        /*
         * try to open the file and read the first byte to determine
         * the file type and create the correct reader object
         */

        File file = new File(uri);
        if (!file.exists()) {
            throw new SeqStoreException("No such file: " + uri);
        }

        char[] cbuf = new char[4];
        try {
            FileReader fr = new FileReader(uri);
            fr.read(cbuf, 0, 4);
            fr.close();
        } catch (FileNotFoundException ex) {
            throw new SeqStoreException("Sequence file " + uri + " missing");
        } catch (IOException ex) {
            throw new SeqStoreException("Could not read sequence file");
        }

        SeqReaderI ret = null;
        switch (cbuf[0]) {
            case '>':
                ret = new FastaReader(uri);
                break;
            case '@':
                ret = new FASTQReader(uri);
                break;
            case 'N':
                ret = new CSFReader(uri);
                break;
            default:
                throw new SeqStoreException("Unsupported file type (" + new String(cbuf) + ")");
        }

        return ret;
    }
}
