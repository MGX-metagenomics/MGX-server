package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.model.db.AttributeType;
import de.cebitec.mgx.model.db.JobState;
import de.cebitec.mgx.util.DBIterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public class AttributeTypeDAO<T extends AttributeType> extends DAO<T> {

    @Override
    Class getType() {
        return AttributeType.class;
    }

    /*
     *  returns attribute types for a job, if it's in FINISHED state
     */
    public DBIterator<AttributeType> ByJob(long jobId) {

        DBIterator<AttributeType> iter = null;

        final String sql = "SELECT atype.id as atype_id, atype.name as atype_name, atype.value_type, atype.structure "
                + "FROM attributetype atype "
                + "LEFT JOIN attribute attr ON (atype.id = attr.attrtype_id) "
                + "LEFT JOIN job ON (attr.job_id = job.id) "
                + "WHERE job.id=? AND job.job_state=? "
                + "GROUP BY atype.id, atype.name, atype.value_type, atype.structure "
                + "ORDER BY atype.name ASC";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rset = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setLong(1, jobId);
            stmt.setInt(2, JobState.FINISHED.getValue());
            rset = stmt.executeQuery();

            iter = new DBIterator<AttributeType>(rset, stmt, c) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };
        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        }

        return iter;
    }

    public DBIterator<AttributeType> BySeqRun(long seqrunId) {

        DBIterator<AttributeType> iter = null;

        final String sql = "SELECT DISTINCT atype.id, atype.name, atype.value_type, atype.structure "
                + "FROM job "
                + "JOIN attribute attr ON (job.id = attr.job_id) "
                + "JOIN attributetype atype ON (attr.attrtype_id = atype.id) "
                + "WHERE job.job_state=? AND job.seqrun_id=?";

        Connection c = getConnection();
        PreparedStatement stmt = null;
        ResultSet rset = null;
        try {
            stmt = c.prepareStatement(sql);
            stmt.setInt(1, JobState.FINISHED.getValue());
            stmt.setLong(2, seqrunId);
            rset = stmt.executeQuery();

            iter = new DBIterator<AttributeType>(rset, stmt, c) {
                @Override
                public AttributeType convert(ResultSet rs) throws SQLException {
                    AttributeType aType = new AttributeType();
                    aType.setId(rs.getLong(1));
                    aType.setName(rs.getString(2));
                    aType.setValueType(rs.getString(3).charAt(0));
                    aType.setStructure(rs.getString(4).charAt(0));
                    return aType;
                }
            };

        } catch (SQLException ex) {
            getController().log(ex.getMessage());
        }

        return iter;
    }
}
