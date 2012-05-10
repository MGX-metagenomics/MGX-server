//package de.cebitec.mgx.seqstorage;
//
//import de.cebitec.mgx.sequence.DNASequenceI;
//import de.cebitec.mgx.sequence.SeqReaderI;
//import de.cebitec.mgx.sequence.SeqStoreException;
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
//        SeqReaderI reader = new FastaReader("/tmp/test.fas");
//        long cnt = 1;
//        while (reader.hasMoreElements()) {
//            DNASequenceI s = reader.nextElement();
//            s.setId(cnt);
//            cnt++;
//        }
//        reader.close();
//
////     
////        
////        for (long test=1; test <= 1260000; test++) {
////            byte[] ret = ByteUtils.longToBytes(test);
////            long foo = ByteUtils.bytesToLong(ret);
////            if (test > 1259900) {
////            System.out.println(test + " -> " + foo); }
////        }
//        
//    }
//}