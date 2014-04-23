package de.cebitec.mgx.web;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.dto.dto.MGXLongList;
import de.cebitec.mgx.dto.dto.MGXMatrixDTO;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.PCAResultDTO;
import de.cebitec.mgx.dto.dto.PointDTOList;
import de.cebitec.mgx.dto.dto.ProfileDTO;
import de.cebitec.mgx.dtoadapter.MatrixDTOFactory;
import de.cebitec.mgx.dtoadapter.PCAResultDTOFactory;
import de.cebitec.mgx.dtoadapter.PointDTOFactory;
import de.cebitec.mgx.model.misc.Matrix;
import de.cebitec.mgx.model.misc.NamedVector;
import de.cebitec.mgx.model.misc.PCAResult;
import de.cebitec.mgx.statistics.Clustering;
import de.cebitec.mgx.statistics.PCA;
import de.cebitec.mgx.statistics.Rarefaction;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.Point;
import de.cebitec.mgx.web.exception.MGXWebException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 *
 * @author sj
 */
@Path("Statistics")
@Stateless
public class StatisticsBean {

    @EJB
    Rarefaction rarefaction;
    @EJB
    Clustering clust;
    @EJB
    PCA pca;
    
    @PUT
    @Path("Rarefaction")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PointDTOList rarefy(MGXLongList dto) {
        double[] data = new double[dto.getLongCount()];
        for (int i = 0; i < dto.getLongCount(); i++) {
            data[i] = (double) dto.getLong(i);
        }
        AutoCloseableIterator<Point> ret;
        try {
            ret = rarefaction.rarefy(data);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PointDTOFactory.getInstance().toDTOList(ret);
    }

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
    @Path("PCA/")
    @Consumes("application/x-protobuf")
    @Produces("application/x-protobuf")
    public PCAResultDTO PCA(MGXMatrixDTO dto) {
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
            ret = pca.pca(m);
        } catch (MGXException ex) {
            throw new MGXWebException(ex.getMessage());
        }
        return PCAResultDTOFactory.getInstance().toDTO(ret);
    }
}
