package de.cebitec.mgx.controller.boot;

import de.cebitec.gpms.core.DataSourceI;
import de.cebitec.gpms.core.DataSource_DBI;
import de.cebitec.gpms.core.GPMSException;
import de.cebitec.gpms.core.MasterI;
import de.cebitec.gpms.core.MembershipI;
import de.cebitec.gpms.data.JDBCMasterI;
import de.cebitec.gpms.util.GPMSDataSourceSelector;
import java.util.logging.Level;

/**
 *
 * @author sjaenick
 */
public class MGX2DataSourceSelector extends GPMSDataSourceSelector {

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
                if ("MGX-2".equals(dsdb.getType().getName()) && "MGX".equals(dsdb.getAPIType().getName())) {
                    return dsdb;
                } else {
                    LOG.log(Level.INFO, "Skipping datasource type {0} with API type {1}", new Object[]{dsdb.getType().getName(), dsdb.getAPIType().getName()});
                }
            }
        }
        return null;
    }

}
