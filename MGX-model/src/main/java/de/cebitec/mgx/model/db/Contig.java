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
    private long length_bp;
    private String bam_file;
    private long bin_id;

    public Contig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length_bp;
    }

    public void setLength(long length_bp) {
        this.length_bp = length_bp;
    }

    public String getBAMFile() {
        return bam_file;
    }

    public void setBAMFile(String bam_file) {
        this.bam_file = bam_file;
    }

    public long getBinId() {
        return bin_id;
    }

    public void setBinId(long bin_id) {
        this.bin_id = bin_id;
    }
    
    
    
}
