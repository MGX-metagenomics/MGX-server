/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.cebitec.mgx.model.misc;

/**
 *
 * @author sj
 */
public class BinSearchResult {

    private final long contig_id;
    private final String contig_name;
    private final long region_id;
    private final String attr_name;
    private final String attrtype_value;

    public BinSearchResult(long contig_id, String contig_name, long region_id, String attr_name, String attrtype_value) {
        this.contig_id = contig_id;
        this.contig_name = contig_name;
        this.region_id = region_id;
        this.attr_name = attr_name;
        this.attrtype_value = attrtype_value;
    }

    public long getContigId() {
        return contig_id;
    }

    public String getContigName() {
        return contig_name;
    }

    public long getRegionId() {
        return region_id;
    }

    public String getAttributeName() {
        return attr_name;
    }

    public String getAttributeTypeValue() {
        return attrtype_value;
    }

}
