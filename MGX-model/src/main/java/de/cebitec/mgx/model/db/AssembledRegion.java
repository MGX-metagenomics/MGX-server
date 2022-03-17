/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.db;

import de.cebitec.mgx.common.RegionType;

/**
 *
 * @author sj
 */
public class AssembledRegion extends Identifiable {

    private int coverage;
    private int start, stop;
    private long contig_id;
    private RegionType type;

    public AssembledRegion() {
    }

    public int getCoverage() {
        return coverage;
    }

    public void setCoverage(int coverage) {
        this.coverage = coverage;
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

    public long getContigId() {
        return contig_id;
    }

    public void setContigId(long contig_id) {
        this.contig_id = contig_id;
    }

    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }
}
