package de.cebitec.mgx.model.misc;

import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class Matrix {

    private final String[] columnNames;
    private final Set<NamedVector> rows;

    public Matrix(String[] columnNames, Set<NamedVector> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Set<NamedVector> getRows() {
        return rows;
    }
}
