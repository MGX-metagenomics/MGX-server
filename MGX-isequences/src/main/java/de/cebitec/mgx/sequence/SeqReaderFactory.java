package de.cebitec.mgx.sequence;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 *
 * @author sjaenick
 */
public class SeqReaderFactory {

    private static ServiceLoader<FactoryI> loader = ServiceLoader.load(FactoryI.class);

    public static SeqReaderI getReader(String filename) throws SeqStoreException {
        return get().getReader(filename);
    }

    public static void delete(String dBFile) {
        try {
            getReader(dBFile).delete();
        } catch (SeqStoreException ex) {
        }
    }

    private static FactoryI get() {
        Iterator<FactoryI> ps = loader.iterator();
        while (ps.hasNext()) {
            FactoryI sr = ps.next();
            if (sr != null) {
                return sr;
            }
        }
        return null;
    }
}
