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
public class Assembly extends Identifiable {

    private String name;
    private long reads_assembled;
    private long total_length_bp;
    private long asmjob_id;

    public Assembly() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getReadsAssembled() {
        return reads_assembled;
    }

    public void setReadsAssembled(long reads_assembled) {
        this.reads_assembled = reads_assembled;
    }

    public long getAsmjobId() {
        return asmjob_id;
    }

    public void setAsmjobId(long asmjob_id) {
        this.asmjob_id = asmjob_id;
    }

    public long getTotalLengthBp() {
        return total_length_bp;
    }

    public void setTotalLengthBp(long total_length_bp) {
        this.total_length_bp = total_length_bp;
    }

}
