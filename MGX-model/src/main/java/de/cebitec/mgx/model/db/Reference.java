package de.cebitec.mgx.model.db;

/**
 *
 * @author belmann
 */
public class Reference extends Identifiable {

    private String name;
    private int length;
    private String filePath;

    public String getFile() {
        return filePath;
    }

    public void setFile(String filePath) {
        this.filePath = filePath;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
