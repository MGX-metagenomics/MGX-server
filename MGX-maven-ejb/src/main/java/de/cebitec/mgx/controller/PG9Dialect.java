package de.cebitec.mgx.controller;

import java.sql.SQLException;
import java.sql.Types;
import javax.persistence.PersistenceException;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.id.SequenceIdentityGenerator;

/**
 *
 * @author sjaenick
 */
public class PG9Dialect extends PostgreSQLDialect {

    public PG9Dialect() {
        super();
        //registerColumnType(Types.BIGINT, "serial8");
    }

    @Override
    public Class getNativeIdentifierGeneratorClass() {
        return SequenceIdentityGenerator.class;
    }

    @Override
    public String getIdentityColumnString(int type) {
        return type == Types.BIGINT
                ? "bigserial not null"
                : "serial not null";
    }

    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTER;
    }
    private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

        @Override
        public String extractConstraintName(SQLException sqle) {

            try {
                int sqlState = Integer.valueOf(JDBCExceptionHelper.extractSqlState(sqle)).intValue();
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
                    // TODO: RESTRICT VIOLATION
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
}
