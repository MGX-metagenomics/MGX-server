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
public class GeneCoverage extends Identifiable {

    private int coverage;
    private long gene_id;
    private long run_id;

    public GeneCoverage() {
    }

    public GeneCoverage(int coverage, long gene_id, long run_id) {
        this.coverage = coverage;
        this.gene_id = gene_id;
        this.run_id = run_id;
    }

    public int getCoverage() {
        return coverage;
    }

    public void setCoverage(int coverage) {
        this.coverage = coverage;
    }

    public long getGeneId() {
        return gene_id;
    }

    public void setGeneId(long gene_id) {
        this.gene_id = gene_id;
    }

    public long getRunId() {
        return run_id;
    }

    public void setRunId(long run_id) {
        this.run_id = run_id;
    }

  

   
    
}
