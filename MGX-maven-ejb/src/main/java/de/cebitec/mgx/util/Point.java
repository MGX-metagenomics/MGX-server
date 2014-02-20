package de.cebitec.mgx.util;

/**
 *
 * @author sj
 */
public class Point {

    private final double[] data = new double[2];

    public Point(double x, double y) {
        data[0] = x;
        data[1] = y;
    }

    public double getX() {
        return data[0];
    }

    public double getY() {
        return data[1];
    }

}
