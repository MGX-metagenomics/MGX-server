package de.cebitec.mgx.download;

import com.google.protobuf.ByteString;
import de.cebitec.gpms.util.GPMSManagedConnectionI;
import de.cebitec.gpms.util.GPMSManagedDataSourceI;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dnautils.DNAUtils;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.seqcompression.FourBitEncoder;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author sjaenick
 */
public class GeneByAttributeDownloadProvider implements DownloadProviderI<SequenceDTOList>, Runnable {

    private static final String GET_GENES = "SELECT g.id, c.name, g.start, g.stop, c.bin_id, b.assembly_id "
            + "FROM gene_observation o JOIN gene g ON (o.gene_id = g.id) "
            + "LEFT JOIN contig c ON (g.contig_id=c.id) "
            + "LEFT JOIN bin b ON (c.bin_id=b.id) "
            + "WHERE o.attr_id IN (?";
    private final GPMSManagedDataSourceI dataSource;
    private final String projectName;
    private Connection conn = null;
    private PreparedStatement stmt = null;
    private ResultSet rs = null;
    protected long lastAccessed;
    private boolean have_more_data = true;
    private final long[] attributeIDs;
    private final File assemblyDir;
    protected int maxSeqsPerChunk = 200;
    private long currentBinId = -1;
    private String currentContigName = null;
    private String currentContigSeq = null;
    private IndexedFastaSequenceFile ifsf = null;
    //
    protected volatile MGXException exception = null;
    protected final Lock lock = new ReentrantLock();
    protected volatile SequenceDTOList nextChunk = null;

    public GeneByAttributeDownloadProvider(GPMSManagedDataSourceI dataSource, String projectName, long[] attributeIDs, File assemblyDir) {
        this.dataSource = dataSource;
        this.projectName = projectName;
        dataSource.subscribe(this);
        this.attributeIDs = attributeIDs;
        this.assemblyDir = assemblyDir;
        this.lastAccessed = System.currentTimeMillis();
    }

    @Override
    public SequenceDTOList fetch() throws MGXException {

        if (exception != null) {
            throw exception;
        }

        lock.lock();

        if (rs == null) {
            //
            // first invocation, perform DB query
            //
            try {
                conn = getConnection();
                String sql = buildSQLQuery(attributeIDs.length);
                stmt = conn.prepareStatement(sql);
                int pos = 1;
                for (long id : attributeIDs) {
                    stmt.setLong(pos++, id);
                }
                rs = stmt.executeQuery();
            } catch (SQLException ex) {
                lock.unlock();
                throw new MGXException(ex);
            }

        }

        if (nextChunk == null) {
            // if run() did not run and produce a new chunk, we synchronously
            // invoke it to obtain the data; race conditions avoided via the
            // lock
            run();
        }
        SequenceDTOList ret = nextChunk;
        nextChunk = null;
        lock.unlock();
        return ret;
    }

    @Override
    public void run() {

        lock.lock();

        if (nextChunk == null) {

            SequenceDTOList.Builder listBuilder = SequenceDTOList.newBuilder();
            int count = 0;
            try {
                while (count < maxSeqsPerChunk && rs.next()) {
                    long geneId = rs.getLong(1);
                    String contigName = rs.getString(2);
                    int start = rs.getInt(3);
                    int stop = rs.getInt(4);
                    long binId = rs.getLong(5);
                    long assemblyId = rs.getLong(6);

                    if (binId != currentBinId) {
                        File asmSubdir = new File(assemblyDir, String.valueOf(assemblyId));
                        File binFasta = new File(asmSubdir, String.valueOf(binId) + ".fna");
                        if (ifsf != null) {
                            ifsf.close();
                        }
                        ifsf = new IndexedFastaSequenceFile(binFasta);
                        currentBinId = binId;
                    }

                    if (!contigName.equals(currentContigName)) {
                        ReferenceSequence ctg = ifsf.getSequence(contigName);
                        currentContigSeq = new String(ctg.getBases());
                        currentContigName = contigName;
                    }

                    String geneSeq;

                    if (start < stop) {
                        geneSeq = currentContigSeq.substring(start, stop);
                    } else {
                        geneSeq = DNAUtils.reverseComplement(currentContigSeq.substring(stop, start));
                    }

                    SequenceDTO seqdto = SequenceDTO.newBuilder()
                            .setId(geneId)
                            .setName(contigName + "_" + String.valueOf(geneId))
                            .setSequence(ByteString.copyFrom(FourBitEncoder.encode(geneSeq.getBytes())))
                            .build();
                    listBuilder.addSeq(seqdto);

                    count++;
                }
            } catch (IOException | SQLException ex) {
                exception = new MGXException(ex);
            }
            have_more_data = count == maxSeqsPerChunk;

            lastAccessed = System.currentTimeMillis();
            listBuilder.setComplete(!have_more_data);
            nextChunk = listBuilder.build();

        }
        lock.unlock();
    }

    @Override
    public void cancel() {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ex) {
        }
        dataSource.close(this);
    }

    @Override
    public void close() throws MGXException {
        try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException ex) {
        }
        dataSource.close(this);
    }

    protected GPMSManagedConnectionI getConnection() throws SQLException {
        return dataSource.getConnection(this);
    }

    @Override
    public long lastAccessed() {
        return lastAccessed;
    }

    @Override
    public String getProjectName() {
        lastAccessed = System.currentTimeMillis();
        return projectName;
    }

    private static String buildSQLQuery(int numElements) {
        StringBuilder sb = new StringBuilder(GET_GENES);
        for (int i = 1; i < numElements; i++) {
            sb.append(",?");
        }
        sb.append(") ORDER BY g.id ASC");
        return sb.toString();
    }
}
