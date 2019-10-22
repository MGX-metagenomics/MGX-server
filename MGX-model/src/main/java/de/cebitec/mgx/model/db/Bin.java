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
    private float completeness, contamination;
    private String taxonomy;
    private long n50;
    private long assembly_id;
    private int predicted_cds;
    private long total_bp;
    private int num_contigs = 0;

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

    public float getContamination() {
        return contamination;
    }
    
    public void setContamination(float contamination) {
        this.contamination = contamination;
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
    
    public void setTotalBp(long bp) {
        this.total_bp = bp;
    }

    public long getTotalBp() {
        return total_bp;
    }

    public int getNumContigs() {
        return num_contigs;
    }

    public void setNumContigs(int num_contigs) {
        this.num_contigs = num_contigs;
    }

}
