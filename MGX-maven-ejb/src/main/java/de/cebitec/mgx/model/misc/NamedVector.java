/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.misc;

/**
 *
 * @author sjaenick
 */
public class NamedVector {

    private final String name;

    private final long[] data;

    public NamedVector(String name, long[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public long[] getData() {
        return data;
    }

}
