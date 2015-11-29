package de.cebitec.mgx.statistics.data;

/**
 *
 * @author sj
 */
public class Point {

    private final double[] data = new double[2];
    private final String name;

    public Point(double x, double y, String name) {
        data[0] = x;
        data[1] = y;
        this.name = name;
    }

    public Point(double x, double y) {
        this(x, y, null);
    }

    public double getX() {
        return data[0];
    }

    public double getY() {
        return data[1];
    }
    
    public String getName() {
        return name;
    }

}
