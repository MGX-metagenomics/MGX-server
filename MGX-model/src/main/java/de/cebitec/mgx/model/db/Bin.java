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
    private String taxonomy;
    private long n50;
    private long assembly_id;
    private int predicted_cds;

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

    public String getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(String taxonomy) {
        this.taxonomy = taxonomy;
    }

    public long getN50() {
        return n50;
    }

    public void setN50(long n50) {
        this.n50 = n50;
    }

    public int getPredictedCDS() {
        return predicted_cds;
    }

    public void setPredictedCDS(int predicted_cds) {
        this.predicted_cds = predicted_cds;
    }
    
    
    
}
