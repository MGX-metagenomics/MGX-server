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
public class Bin extends Identifiable {
    
    private String name;
    private float completeness;
    private long assembly_id;

    public Bin() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCompleteness() {
        return completeness;
    }

    public void setCompleteness(float completeness) {
        this.completeness = completeness;
    }

    public long getAssemblyId() {
        return assembly_id;
    }

    public void setAssemblyId(long assembly_id) {
        this.assembly_id = assembly_id;
    }
    
}
