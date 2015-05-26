package de.cebitec.mgx.controller;

import java.sql.SQLException;
import java.sql.Types;
import javax.persistence.PersistenceException;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.id.SequenceIdentityGenerator;

/**
 *
 * @author sjaenick
 */
public class PG9Dialect extends PostgreSQL82Dialect {

    public PG9Dialect() {
        super();
        registerColumnType(Types.BIGINT, "serial8");
    }

    @Override
    public Class getNativeIdentifierGeneratorClass() {
        return SequenceIdentityGenerator.class;
    }

    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTER;
    }
    private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

        @Override
        public String extractConstraintName(SQLException sqle) {

            try {
                int sqlState = Integer.valueOf(extractSqlState(sqle));
                switch (sqlState) {
                    // CHECK VIOLATION
                    case 23514:
                        return extractUsingTemplate("violates check constraint \"", "\"", sqle.getMessage());
                    // UNIQUE VIOLATION
                    case 23505:
                        while (sqle.getNextException() != null) {
                            sqle = sqle.getNextException();
                        }
                        String msg = extractUsingTemplate("Detail: ", ".", sqle.getMessage());
                        throw new PersistenceException(new SQLException(msg));
                    //return msg;
                    // FOREIGN KEY VIOLATION
                    case 23503:
                        return extractUsingTemplate("violates foreign key constraint \"", "\"", sqle.getMessage());
                    // NOT NULL VIOLATION
                    case 23502:
                        return extractUsingTemplate("null value in column \"", "\" violates not-null constraint", sqle.getMessage());
                    // RESTRICT VIOLATION
                    case 23001:
                        return null;
                    // ALL OTHER
                    default:
                        return null;
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    };

    /**
     * For the given SQLException, locates the X/Open-compliant SQLState.
     *
     * @param sqlException The exception from which to extract the SQLState
     * @return The SQLState code, or null.
     * 
     * Code duplicated from Hibernate's JdbcExceptionHelper since it resides
     * within an internal package.
     * 
     */
    private static String extractSqlState(SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        SQLException nested = sqlException.getNextException();
        while (sqlState == null && nested != null) {
            sqlState = nested.getSQLState();
            nested = nested.getNextException();
        }
        return sqlState;
    }
}
