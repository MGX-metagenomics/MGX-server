/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.db;

/**
 *
 * @author sj
 */
public class Contig extends Identifiable {

    private String name;
    private float gc;
    private int length_bp;
    private long bin_id;

    public Contig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length_bp;
    }

    public void setLength(int length_bp) {
        this.length_bp = length_bp;
    }

    public float getGC() {
        return gc;
    }

    public void setGC(float gc) {
        this.gc = gc;
    }

    public long getBinId() {
        return bin_id;
    }

    public void setBinId(long bin_id) {
        this.bin_id = bin_id;
    }
}
