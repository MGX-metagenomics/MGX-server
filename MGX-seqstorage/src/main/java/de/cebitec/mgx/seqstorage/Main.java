//package de.cebitec.mgx.seqstorage;
//
//import de.cebitec.mgx.seqstorage.encoding.ByteUtils;
//import java.io.IOException;
//
///**
// *
// * @author sjaenick
// */
//public class Main {
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) throws SeqStoreException, IOException {
//        SeqReaderI reader = SeqReaderFactory.getReader("/tmp/test.fas");
//        SeqWriterI writer = new CSFWriter("/tmp/test");
//        long cnt = 1;
//        while (reader.hasMoreElements()) {
//            DNASequenceI s = reader.nextElement();
//            s.setId(cnt);
//            writer.addSequence(s);
//            cnt++;
//        }
//        reader.close();
//        writer.close();
//
//        reader = SeqReaderFactory.getReader("/tmp/test");
//        while (reader.hasMoreElements()) {
//            DNASequenceI s = reader.nextElement();
//            System.out.println(s.getId());
//        }
//        reader.close();
//        
//        for (long test=1; test <= 1260000; test++) {
//            byte[] ret = ByteUtils.longToBytes(test);
//            long foo = ByteUtils.bytesToLong(ret);
//            if (test > 1259900) {
//            System.out.println(test + " -> " + foo); }
//        }
//        
//    }
//}