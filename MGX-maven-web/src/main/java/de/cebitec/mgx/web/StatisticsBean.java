package de.cebitec.mgx.web;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.dto.dto.MGXDouble;
import de.cebitec.mgx.dto.dto.MGXDoubleList;
import de.cebitec.mgx.dto.dto.MGXMatrixDTO;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.PCAResultDTO;
import de.cebitec.mgx.dto.dto.PointDTOList;
import de.cebitec.mgx.dtoadapter.MatrixDTOFactory;
import de.cebitec.mgx.dtoadapter.PCAResultDTOFactory;
import de.cebitec.mgx.dtoadapter.PointDTOFactory;
import de.cebitec.mgx.statistics.Clustering;
import de.cebitec.mgx.statistics.DataTransform;
import de.cebitec.mgx.statistics.Distance;
import de.cebitec.mgx.statistics.NMDS;
import de.cebitec.mgx.statistics.PCA;
import de.cebitec.mgx.statistics.data.Matrix;
import de.cebitec.mgx.statistics.data.NamedVector;
import de.cebitec.mgx.statistics.data.PCAResult;
import de.cebitec.mgx.statistics.data.Point;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.web.exception.MGXWebException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 *
 * @author sj
 */
@Path("Statistics")
@Stateless
public class StatisticsBean {

    @EJB
    Clustering clust;
    @EJB
    PCA pca;
    @EJB
    NMDS nmds;
    @EJB
    DataTransform transform;
    @EJB
    Distance distance;

    @PUT
    @Path("Clustering/{dist}/{agglo}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXString cluster(MGXMatrixDTO dto, @PathParam("dist") String distMethod, @PathParam("agglo") String aggloMethod) {
        Matrix m = MatrixDTOFactory.getInstance().toDB(dto);

        String ret;
        try {
            ret = clust.cluster(m, distMethod, aggloMethod);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXString.newBuilder().setValue(ret).build();
    }
    
    @PUT
    @Path("NewickToSVG")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXString newickToSVG(MGXString dto) {

        String ret;
        
        try {
            ret = clust.newickToSVG(dto.getValue());
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return MGXString.newBuilder().setValue(ret).build();
    }

    @PUT
    @Path("PCA/{pc1}/{pc2}")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PCAResultDTO PCA(MGXMatrixDTO dto, @PathParam("pc1") Integer pc1, @PathParam("pc2") Integer pc2) {
        if (pc1 < 1 || pc1 > 3 || pc2 < 1 || pc2 > 3) {
            throw new MGXWebException("Invalid PC requested");
        }
        Matrix m = MatrixDTOFactory.getInstance().toDB(dto);

        int numVars = m.getColumnNames().length;
        for (NamedVector nv : m.getRows()) {
            if (nv.getData().length != numVars) {
                throw new MGXWebException(String.format("Error in data matrix: %d variables, got vector with %d elements",
                        numVars, nv.getData().length));
            }
        }

        PCAResult ret;
        try {
            ret = pca.pca(m, pc1, pc2);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PCAResultDTOFactory.getInstance().toDTO(ret);
    }

    @PUT
    @Path("NMDS")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PointDTOList NMDS(MGXMatrixDTO dto) {
        Matrix m = MatrixDTOFactory.getInstance().toDB(dto);

        int numVars = m.getColumnNames().length;
        for (NamedVector nv : m.getRows()) {
            if (nv.getData().length != numVars) {
                throw new MGXWebException(String.format("Error in data matrix: %d variables, got vector with %d elements",
                        numVars, nv.getData().length));
            }
        }

        AutoCloseableIterator<Point> ret;
        try {
            ret = nmds.nmds(m);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PointDTOFactory.getInstance().toDTOList(ret);
    }

    @PUT
    @Path("toCLR")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXDoubleList toCLR(MGXDoubleList dto) {
        double[] in = new double[dto.getValueCount()];
        for (int i = 0; i < dto.getValueCount(); i++) {
            in[i] = dto.getValue(i);
        }
        double[] clr;
        try {
            clr = transform.clr(in);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        MGXDoubleList.Builder b = MGXDoubleList.newBuilder();
        for (double d : clr) {
            b = b.addValue(d);
        }
        return b.build();
    }

    @PUT
    @Path("aitchisonDistance")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public MGXDouble aitchisonDistance(MGXMatrixDTO dto) {
        if (2 != dto.getRowCount()) {
            throw new MGXWebException("Invalid number of rows in matrix.");
        }

        MGXDoubleList row1 = dto.getRow(0).getValues();
        MGXDoubleList row2 = dto.getRow(1).getValues();

        if (row1.getValueCount() != row2.getValueCount()) {
            throw new MGXWebException("Rows are required to have equal lengths.");
        }

        double[] r1 = new double[row1.getValueCount()];
        double[] r2 = new double[row2.getValueCount()];
        for (int i = 0; i < r1.length; i++) {
            r1[i] = row1.getValue(i);
            r2[i] = row2.getValue(i);
        }

        double aiDist;
        try {
            aiDist = distance.aitchisonDistance(r1, r2);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }

        return MGXDouble.newBuilder().setValue(aiDist).build();
    }
}
