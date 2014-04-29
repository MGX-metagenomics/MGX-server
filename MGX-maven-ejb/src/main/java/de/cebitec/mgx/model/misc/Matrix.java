package de.cebitec.mgx.model.misc;

import java.util.List;

/**
 *
 * @author sjaenick
 */
public class Matrix {

    private final String[] columnNames;
    private final List<NamedVector> rows;

    public Matrix(String[] columnNames, List<NamedVector> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public List<NamedVector> getRows() {
        return rows;
    }
}
