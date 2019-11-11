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
public class GeneAnnotation extends Identifiable {

    private int start, stop;
    private long gene_id;
    private long attr_id;

    public GeneAnnotation() {
    }

    public GeneAnnotation(long gene_id, long attr_id, int start, int stop) {
        this.gene_id = gene_id;
        this.attr_id = attr_id;
        this.start = start;
        this.stop = stop;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    public long getGeneId() {
        return gene_id;
    }

    public void setGeneId(long gene_id) {
        this.gene_id = gene_id;
    }

    public long getAttributeId() {
        return attr_id;
    }

    public void setAttributeId(long attr_id) {
        this.attr_id = attr_id;
    }
    
}
