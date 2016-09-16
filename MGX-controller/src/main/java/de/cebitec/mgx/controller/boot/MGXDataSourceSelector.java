package de.cebitec.mgx.controller.boot;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.util.GPMSDataSourceSelector;

/**
 *
 * @author sjaenick
 */
public class MGXDataSourceSelector extends GPMSDataSourceSelector {

//    private final static DBAPITypeI MGX_DBAPI_TYPE = new DBAPIType("MGX");
//    private final static DBAPITypeI REST_DBAPI_TYPE = new DBAPIType("REST");
//    private final static DataSourceTypeI MGX_DS_TYPE = new DataSourceType("MGX");
    @Override
    public <T extends MasterI> DataSourceI selectFromDataSources(MembershipI mbr, Class<T> masterClass) throws GPMSException {

        if (!JDBCMasterI.class.isAssignableFrom(masterClass)) {
            throw new GPMSException("MGX requires JDBC as a master class.");
        }
        if (mbr.getProject().getDataSources().isEmpty()) {
            throw new GPMSException(mbr.getProject().getName() + " has no Datasources.");
        }
        for (DataSourceI gpmsDS : mbr.getProject().getDataSources()) {
            if (gpmsDS instanceof DataSource_DBI) {
                DataSource_DBI dsdb = (DataSource_DBI) gpmsDS;
                if ("MGX".equals(dsdb.getType().getName()) && "MGX".equals(dsdb.getAPIType().getName())) {
                    return dsdb;
                } else if ("MGX".equals(dsdb.getType().getName()) && "O2DBI2".equals(dsdb.getAPIType().getName())) {
                    // Bielefeld setup does not have a MGX api type yet
                    return dsdb;
                } else {
                    System.err.println("Skipping datasource type "+dsdb.getType().getName()+ " with API type "+dsdb.getAPIType().getName());
                }
            }
        }
        return null;
    }

}
